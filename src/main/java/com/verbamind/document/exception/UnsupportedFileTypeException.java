package com.verbamind.document.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends ApiException {
    public UnsupportedFileTypeException(String contentType) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Unsupported file type: " + contentType + ". Allowed: PDF, DOCX, TXT");
    }
}