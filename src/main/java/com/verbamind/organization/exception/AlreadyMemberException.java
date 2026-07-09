package com.verbamind.organization.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class AlreadyMemberException extends ApiException {
    public AlreadyMemberException(String email) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "User is already a member: " + email);
    }
}