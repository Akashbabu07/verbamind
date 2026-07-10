package com.verbamind.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameDocumentRequest(
        @NotBlank @Size(min = 1, max = 255) String fileName
) {}