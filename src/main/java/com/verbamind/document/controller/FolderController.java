package com.verbamind.document.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.document.dto.CreateFolderRequest;
import com.verbamind.document.dto.FolderResponse;
import com.verbamind.document.service.FolderService;
import com.verbamind.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> create(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                folderService.create(currentUser.getId(), organizationId, request), "Folder created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID parentFolderId) {
        return ResponseEntity.ok(ApiResponse.success(
                folderService.list(currentUser.getId(), organizationId, parentFolderId)));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID folderId) {
        folderService.delete(currentUser.getId(), organizationId, folderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Folder deleted"));
    }

    @PatchMapping("/documents/{documentId}/move")
    public ResponseEntity<ApiResponse<Void>> moveDocument(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId,
            @RequestParam(required = false) UUID folderId) {
        folderService.moveDocument(currentUser.getId(), organizationId, documentId, folderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document moved"));
    }
}