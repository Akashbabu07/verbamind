package com.verbamind.document.service;

import com.verbamind.document.dto.CreateFolderRequest;
import com.verbamind.document.dto.FolderResponse;
import com.verbamind.document.entity.Document;
import com.verbamind.document.entity.Folder;
import com.verbamind.document.exception.DocumentNotFoundException;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final OrganizationAccessGuard accessGuard;

    public FolderService(FolderRepository folderRepository,
                         DocumentRepository documentRepository,
                         OrganizationAccessGuard accessGuard) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public FolderResponse create(UUID currentUserId, UUID organizationId, CreateFolderRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);

        if (request.parentFolderId() != null) {
            getFolderOrThrow(organizationId, request.parentFolderId());
        }
        if (folderRepository.existsByOrganizationIdAndParentFolderIdAndName(
                organizationId, request.parentFolderId(), request.name())) {
            throw new IllegalArgumentException("A folder with this name already exists here");
        }

        Folder folder = new Folder();
        folder.setOrganizationId(organizationId);
        folder.setParentFolderId(request.parentFolderId());
        folder.setName(request.name());
        folderRepository.save(folder);
        return toResponse(folder);
    }

    public List<FolderResponse> list(UUID currentUserId, UUID organizationId, UUID parentFolderId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        List<Folder> folders = parentFolderId == null
                ? folderRepository.findByOrganizationIdAndParentFolderIdIsNull(organizationId)
                : folderRepository.findByOrganizationIdAndParentFolderId(organizationId, parentFolderId);
        return folders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID currentUserId, UUID organizationId, UUID folderId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        getFolderOrThrow(organizationId, folderId);
        folderRepository.deleteById(folderId);
    }

    @Transactional
    public void moveDocument(UUID currentUserId, UUID organizationId, UUID documentId, UUID folderId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Document doc = documentRepository.findById(documentId)
                .filter(d -> !d.isDeleted() && d.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (folderId != null) {
            getFolderOrThrow(organizationId, folderId);
        }
        doc.setFolderId(folderId);
        documentRepository.save(doc);
    }

    private Folder getFolderOrThrow(UUID organizationId, UUID folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        if (!folder.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Folder not found");
        }
        return folder;
    }

    private FolderResponse toResponse(Folder folder) {
        return new FolderResponse(folder.getId(), folder.getName(), folder.getParentFolderId(), folder.getCreatedAt());
    }
}