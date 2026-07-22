package com.verbamind.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTagRequest(
        @NotBlank @Size(min = 1, max = 50) String tag
) {}