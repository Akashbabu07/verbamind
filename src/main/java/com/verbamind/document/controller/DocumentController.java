package com.verbamind.document.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.document.dto.*;
import com.verbamind.document.service.DocumentService;
import com.verbamind.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam("file") MultipartFile file) {
        var doc = documentService.upload(currentUser.getId(), organizationId, file);
        return ResponseEntity.ok(ApiResponse.success(doc, "Document uploaded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DocumentPageResponse>> list(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                documentService.list(currentUser.getId(), organizationId, pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<DocumentPageResponse>> search(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                documentService.search(currentUser.getId(), organizationId, q, pageable)));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getMetadata(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getMetadata(currentUser.getId(), organizationId, documentId)));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        var result = documentService.download(currentUser.getId(), organizationId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(result.fileName()).build().toString())
                .body(new InputStreamResource(result.stream()));
    }

    @PatchMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> rename(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId,
            @Valid @RequestBody RenameDocumentRequest request) {
        var doc = documentService.rename(currentUser.getId(), organizationId, documentId, request);
        return ResponseEntity.ok(ApiResponse.success(doc, "Document renamed"));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        documentService.delete(currentUser.getId(), organizationId, documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted"));
    }

    @PostMapping("/{documentId}/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(@PathVariable UUID organizationId,
                                                    @PathVariable UUID documentId,
                                                    @RequestBody AddTagRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        documentService.addTag(principal.getId(), organizationId, documentId, request.tag());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag added"));
    }

    @DeleteMapping("/{documentId}/tags/{tag}")
    public ResponseEntity<ApiResponse<Void>> removeTag(@PathVariable UUID organizationId,
                                                       @PathVariable UUID documentId,
                                                       @PathVariable String tag,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        documentService.removeTag(principal.getId(), organizationId, documentId, tag);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag removed"));
    }

    @GetMapping("/{documentId}/preview")
    public ResponseEntity<InputStreamResource> preview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        var result = documentService.preview(currentUser.getId(), organizationId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(result.fileName()).build().toString())
                .body(new InputStreamResource(result.stream()));
    }
    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadNewVersion(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file) {
        var doc = documentService.uploadNewVersion(currentUser.getId(), organizationId, documentId, file);
        return ResponseEntity.ok(ApiResponse.success(doc, "New version uploaded"));
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> listVersions(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.listVersions(currentUser.getId(), organizationId, documentId)));
    }

    @GetMapping("/{documentId}/versions/{versionNumber}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId,
            @PathVariable int versionNumber) {
        var result = documentService.downloadVersion(currentUser.getId(), organizationId, documentId, versionNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(result.fileName()).build().toString())
                .body(new InputStreamResource(result.stream()));
    }
}