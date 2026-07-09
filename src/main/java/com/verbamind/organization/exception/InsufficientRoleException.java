package com.verbamind.organization.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class InsufficientRoleException extends ApiException {
    public InsufficientRoleException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "You do not have permission to perform this action");
    }
}