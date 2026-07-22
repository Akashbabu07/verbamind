package com.verbamind.document.repository;

import com.verbamind.document.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByOrganizationIdAndParentFolderId(UUID organizationId, UUID parentFolderId);
    List<Folder> findByOrganizationIdAndParentFolderIdIsNull(UUID organizationId);
    boolean existsByOrganizationIdAndParentFolderIdAndName(UUID organizationId, UUID parentFolderId, String name);
}