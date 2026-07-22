package com.verbamind.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFolderRequest(
        @NotBlank @Size(min = 1, max = 255) String name,
        UUID parentFolderId
) {}