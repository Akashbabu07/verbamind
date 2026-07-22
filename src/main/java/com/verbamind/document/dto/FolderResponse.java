package com.verbamind.document.dto;

import java.time.Instant;
import java.util.UUID;

public record FolderResponse(UUID id, String name, UUID parentFolderId, Instant createdAt) {}