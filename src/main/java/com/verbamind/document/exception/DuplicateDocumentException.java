package com.verbamind.document.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DuplicateDocumentException extends ApiException {

    private final UUID existingDocumentId;

    public DuplicateDocumentException(UUID existingDocumentId) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "An identical file already exists in this workspace");
        this.existingDocumentId = existingDocumentId;
    }

    public UUID getExistingDocumentId() {
        return existingDocumentId;
    }
}