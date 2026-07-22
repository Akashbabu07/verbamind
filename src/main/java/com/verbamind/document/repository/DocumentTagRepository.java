package com.verbamind.document.repository;

import com.verbamind.document.entity.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, UUID> {
    List<DocumentTag> findByDocumentId(UUID documentId);
    void deleteByDocumentIdAndTag(UUID documentId, String tag);
    List<DocumentTag> findByDocumentIdIn(List<UUID> documentIds);
}