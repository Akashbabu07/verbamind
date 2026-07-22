package com.verbamind.document.service;

import com.verbamind.ai.service.DocumentIngestionService;
import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.document.dto.*;
import com.verbamind.document.entity.Document;
import com.verbamind.document.entity.DocumentStatus;
import com.verbamind.document.entity.DocumentTag;
import com.verbamind.document.entity.DocumentVersion;
import com.verbamind.document.exception.DocumentNotFoundException;
import com.verbamind.document.exception.FileTooLargeException;
import com.verbamind.document.exception.UnsupportedFileTypeException;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.repository.DocumentTagRepository;
import com.verbamind.document.repository.DocumentVersionRepository;
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

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final OrganizationAccessGuard accessGuard;
    private final DocumentIngestionService ingestionService;
    private final UsageService usageService;
    private final DocumentTagRepository tagRepository;
    private final FileHashService fileHashService;
    private final DocumentVersionRepository versionRepository;

    public DocumentService(DocumentRepository documentRepository,
                           OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           OrganizationAccessGuard accessGuard,DocumentIngestionService ingestionService,UsageService usageService,DocumentTagRepository tagRepository, FileHashService fileHashService,DocumentVersionRepository versionRepository) {
        this.documentRepository = documentRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.accessGuard = accessGuard;
        this.ingestionService = ingestionService;
        this.usageService = usageService;
        this.tagRepository = tagRepository;
        this.fileHashService=fileHashService;
        this.versionRepository=versionRepository;
    }

    @Transactional
    public void addTag(UUID currentUserId, UUID organizationId, UUID documentId, String tag) {
        accessGuard.requireMembership(organizationId, currentUserId);
        getDocOrThrow(organizationId, documentId);

        String normalized = tag.trim().toLowerCase();
        if (tagRepository.findByDocumentId(documentId).stream()
                .noneMatch(t -> t.getTag().equals(normalized))) {
            DocumentTag docTag = new DocumentTag();
            docTag.setDocumentId(documentId);
            docTag.setTag(normalized);
            tagRepository.save(docTag);
        }
    }

    @Transactional
    public void removeTag(UUID currentUserId, UUID organizationId, UUID documentId, String tag) {
        accessGuard.requireMembership(organizationId, currentUserId);
        getDocOrThrow(organizationId, documentId);
        tagRepository.deleteByDocumentIdAndTag(documentId, tag.trim().toLowerCase());
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

        String contentHash;
        try (InputStream hashStream = file.getInputStream()) {
            contentHash = fileHashService.sha256(hashStream);
        } catch (Exception e) {
            throw new com.verbamind.document.exception.StorageException("Failed to read uploaded file");
        }

        documentRepository.findByOrganizationIdAndContentHashAndDeletedFalse(organizationId, contentHash)
                .ifPresent(existing -> {
                    throw new com.verbamind.document.exception.DuplicateDocumentException(existing.getId());
                });

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
        doc.setContentHash(contentHash);
        doc.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(doc);

        UUID documentId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ingestionService.processDocument(documentId);
                    }
                }
        );
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

    @Transactional
    public DocumentResponse uploadNewVersion(UUID currentUserId, UUID organizationId, UUID documentId, MultipartFile file) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedFileTypeException(file.getContentType());
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException(MAX_FILE_SIZE);
        }
        usageService.assertStorageQuotaAvailable(organizationId, file.getSize());

        DocumentVersion snapshot = new DocumentVersion();
        snapshot.setDocumentId(doc.getId());
        snapshot.setVersionNumber(doc.getCurrentVersion());
        snapshot.setStorageKey(doc.getStorageKey());
        snapshot.setFileSize(doc.getFileSize());
        snapshot.setContentHash(doc.getContentHash());
        snapshot.setUploadedBy(doc.getOwner().getId());
        versionRepository.save(snapshot);


        ingestionService.deleteEmbeddings(documentId);

        String contentHash;
        String newStorageKey = organizationId + "/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        try (InputStream hashStream = file.getInputStream()) {
            contentHash = fileHashService.sha256(hashStream);
        } catch (Exception e) {
            throw new com.verbamind.document.exception.StorageException("Failed to read uploaded file");
        }

        documentRepository.findByOrganizationIdAndContentHashAndDeletedFalse(organizationId, contentHash)
                .ifPresent(existing -> {
                    throw new com.verbamind.document.exception.DuplicateDocumentException(existing.getId());
                });

        try (InputStream in = file.getInputStream()) {
            storageService.upload(newStorageKey, in, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new com.verbamind.document.exception.StorageException("Failed to read uploaded file");
        }

        doc.setStorageKey(newStorageKey);
        doc.setFileSize(file.getSize());
        doc.setContentHash(contentHash);
        doc.setCurrentVersion(doc.getCurrentVersion() + 1);
        doc.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(doc);

        UUID docId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ingestionService.processDocument(docId);
                    }
                }
        );

        return toResponse(doc);
    }

    public List<DocumentVersionResponse> listVersions(UUID currentUserId, UUID organizationId, UUID documentId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);
        List<DocumentVersionResponse> history = versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(v -> new DocumentVersionResponse(v.getId(), v.getVersionNumber(), v.getFileSize(), v.getCreatedAt(), false))
                .collect(java.util.stream.Collectors.toList());
        history.add(0, new DocumentVersionResponse(doc.getId(), doc.getCurrentVersion(), doc.getFileSize(), doc.getCreatedAt(), true));
        return history;
    }

    public DownloadResult downloadVersion(UUID currentUserId, UUID organizationId, UUID documentId, int versionNumber) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);

        if (versionNumber == doc.getCurrentVersion()) {
            return download(currentUserId, organizationId, documentId);
        }

        DocumentVersion version = versionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new DocumentNotFoundException("Version not found"));
        InputStream stream = storageService.download(version.getStorageKey());
        return new DownloadResult(stream, doc.getOriginalFileName(), doc.getContentType());
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

    public DownloadResult preview(UUID currentUserId, UUID organizationId, UUID documentId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = getDocOrThrow(organizationId, documentId);

        if (!ALLOWED_CONTENT_TYPES.contains(doc.getContentType())) {
            throw new UnsupportedFileTypeException(doc.getContentType());
        }

        InputStream stream = storageService.download(doc.getStorageKey());
        return new DownloadResult(stream, doc.getOriginalFileName(), doc.getContentType());
    }
}