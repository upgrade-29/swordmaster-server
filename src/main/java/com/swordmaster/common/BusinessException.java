package com.swordmaster.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String     code;      // Null 허용 (ErrorResponse의 code)

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, null, message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);

        this.status = status;
        this.code   = code;
    }
}
