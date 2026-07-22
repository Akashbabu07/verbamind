package com.verbamind.document.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_tags", uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "tag"}))
public class DocumentTag {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false, length = 50)
    private String tag;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}