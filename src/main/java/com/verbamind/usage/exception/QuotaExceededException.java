package com.verbamind.usage.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class QuotaExceededException extends ApiException {
    public QuotaExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.QUOTA_EXCEEDED, message);
    }
}