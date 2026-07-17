package com.verbamind.ai.repository;

import com.verbamind.ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying
    void deleteByDocumentId(UUID documentId);

    @Query(value = """
            SELECT * FROM document_chunks
            WHERE organization_id = :organizationId
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(@Param("organizationId") UUID organizationId,
                                          @Param("embedding") String embedding,
                                          @Param("topK") int topK);
}