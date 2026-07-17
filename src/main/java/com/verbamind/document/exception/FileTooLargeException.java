package com.verbamind.document.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class FileTooLargeException extends ApiException {
    public FileTooLargeException(long maxBytes) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "File exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + " MB");
    }
}