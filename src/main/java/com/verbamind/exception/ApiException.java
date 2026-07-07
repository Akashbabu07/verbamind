package com.verbamind.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ApiException extends RuntimeException{
    private HttpStatus httpStatus;
    private ErrorCode errorCode;
    public ApiException(HttpStatus httpStatus, ErrorCode errorCode,String message){
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

}
