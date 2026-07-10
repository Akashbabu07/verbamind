package com.verbamind.document.repository;

import com.verbamind.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByOrganizationIdAndDeletedFalse(UUID organizationId, Pageable pageable);

    @Query("""
           SELECT d FROM Document d
           WHERE d.organization.id = :organizationId
             AND d.deleted = false
             AND LOWER(d.fileName) LIKE LOWER(CONCAT('%', :query, '%'))
           """)
    Page<Document> searchByOrganization(@Param("organizationId") UUID organizationId,
                                        @Param("query") String query,
                                        Pageable pageable);

    long countByOrganizationIdAndDeletedFalse(UUID organizationId);
}