package com.verbamind.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameChatRequest(
        @NotBlank @Size(min = 1, max = 255) String title
) {}