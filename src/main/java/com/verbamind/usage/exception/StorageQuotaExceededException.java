package com.verbamind.usage.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class StorageQuotaExceededException extends ApiException {
    public StorageQuotaExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.QUOTA_EXCEEDED, message);
    }
}