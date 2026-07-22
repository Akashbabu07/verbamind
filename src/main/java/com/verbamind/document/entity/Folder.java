package com.verbamind.document.entity;

import com.verbamind.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(UUID parentFolderId) { this.parentFolderId = parentFolderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
}