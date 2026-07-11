package com.verbamind.ai.dto;

import java.util.List;

public record AskQuestionResponse(
        String answer,
        List<CitationDto> citations
) {}