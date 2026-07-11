package com.verbamind.ai.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class AiProviderException extends ApiException {
    public AiProviderException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR, "AI provider error: " + message);
    }
}