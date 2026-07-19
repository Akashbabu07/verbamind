package com.verbamind.document.service;

import com.verbamind.ai.service.DocumentIngestionService;
import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.document.dto.*;
import com.verbamind.document.entity.Document;
import com.verbamind.document.entity.DocumentStatus;
import com.verbamind.document.exception.DocumentNotFoundException;
import com.verbamind.document.exception.FileTooLargeException;
import com.verbamind.document.exception.UnsupportedFileTypeException;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.exception.OrganizationNotFoundException;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.organization.service.OrganizationService;
import com.verbamind.usage.service.UsageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "text/plain"
    );

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024; // 25 MB — keep in sync with application.yml multipart limits

    private final DocumentRepository documentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final OrganizationAccessGuard accessGuard;
    private final DocumentIngestionService ingestionService;
    private final UsageService usageService;


    public DocumentService(DocumentRepository documentRepository,
                           OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           OrganizationAccessGuard accessGuard,DocumentIngestionService ingestionService,UsageService usageService) {
        this.documentRepository = documentRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.accessGuard = accessGuard;
        this.ingestionService = ingestionService;
        this.usageService = usageService;
    }

    @Transactional
    public DocumentResponse upload(UUID currentUserId, UUID organizationId, MultipartFile file) {
        accessGuard.requireMembership(organizationId, currentUserId);

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedFileTypeException(file.getContentType());
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException(MAX_FILE_SIZE);
        }
        usageService.assertStorageQuotaAvailable(organizationId, file.getSize());

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        User owner = userRepository.findById(currentUserId).orElseThrow();

        String storageKey = organizationId + "/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());

        try (InputStream in = file.getInputStream()) {
            storageService.upload(storageKey, in, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new com.verbamind.document.exception.StorageException("Failed to read uploaded file");
        }

        Document doc = new Document();
        doc.setOrganization(org);
        doc.setOwner(owner);
        doc.setFileName(file.getOriginalFilename());
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStorageKey(storageKey);
        doc.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(doc);

        UUID documentId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // Only fire the async ingestion pipeline once this transaction has
                        // actually committed, so processDocument()'s own transaction can
                        // see the Document row via findById(). Firing this mid-transaction
                        // causes a silent no-op (see processDocument's null guard) because
                        // the row isn't visible to the other thread/connection yet.
                        ingestionService.processDocument(documentId);
                    }
                }
        );
        // ingestionService.processDocument() will flip status UPLOADED -> PROCESSING -> READY/FAILED.

        return toResponse(doc);
    }

    public DocumentPageResponse list(UUID currentUserId, UUID organizationId, Pageable pageable) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Page<Document> page = documentRepository.findByOrganizationIdAndDeletedFalse(organizationId, pageable);
        return toPageResponse(page);
    }

    public DocumentPageResponse search(UUID currentUserId, UUID organizationId, String query, Pageable pageable) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Page<Document> page = documentRepository.searchByOrganization(organizationId, query, pageable);
        return toPageResponse(page);
    }

    public DocumentResponse getMetadata(UUID currentUserId, UUID organizationId, UUID documentId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);
        return toResponse(doc);
    }

    public DownloadResult download(UUID currentUserId, UUID organizationId, UUID documentId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);
        InputStream stream = storageService.download(doc.getStorageKey());
        return new DownloadResult(stream, doc.getOriginalFileName(), doc.getContentType());
    }

    @Transactional
    public DocumentResponse rename(UUID currentUserId, UUID organizationId, UUID documentId, RenameDocumentRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);
        doc.setFileName(request.fileName());
        documentRepository.save(doc);
        return toResponse(doc);
    }

    @Transactional
    public void delete(UUID currentUserId, UUID organizationId, UUID documentId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);
        doc.setDeleted(true);
        documentRepository.save(doc);
        storageService.delete(doc.getStorageKey());

        ingestionService.deleteEmbeddings(documentId);
    }

    private Document getDocOrThrow(UUID organizationId, UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        if (!doc.getOrganization().getId().equals(organizationId)) {
            throw new DocumentNotFoundException("Document not found");
        }
        return doc;
    }

    private String sanitize(String fileName) {
        if (fileName == null) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(), doc.getFileName(), doc.getContentType(), doc.getFileSize(),
                doc.getStatus(), doc.getOwner().getId(), doc.getCreatedAt());
    }

    private DocumentPageResponse toPageResponse(Page<Document> page) {
        List<DocumentResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new DocumentPageResponse(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public record DownloadResult(InputStream stream, String fileName, String contentType) {}
}