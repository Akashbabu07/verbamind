package com.verbamind.ai.controller;

import com.verbamind.ai.dto.AskQuestionRequest;
import com.verbamind.ai.dto.AskQuestionResponse;
import com.verbamind.ai.service.RagQueryService;
import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/ai")
public class AiController {

    private final RagQueryService ragQueryService;

    public AiController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AskQuestionResponse>> ask(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody AskQuestionRequest request) {
        var response = ragQueryService.ask(currentUser.getId(), organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}