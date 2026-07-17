package com.verbamind.ai.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Getter
    @Id
    @GeneratedValue
    private UUID id;

    @Setter
    @Getter
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Setter
    @Getter
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Setter
    @Getter
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Setter
    @Getter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Getter
    @Setter
    @Column(columnDefinition = "vector(768)")
    private PGvector embedding;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

}
