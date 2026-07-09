package com.verbamind.organization.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class OrganizationNotFoundException extends ApiException {
    public OrganizationNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}