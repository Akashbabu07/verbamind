package com.verbamind.organization.exception;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotAMemberException extends ApiException {
    public NotAMemberException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "You are not a member of this organization");
    }
}