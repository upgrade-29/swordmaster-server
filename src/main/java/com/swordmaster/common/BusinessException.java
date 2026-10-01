package com.swordmaster.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);

        status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);

        this.status = status;
    }
}
