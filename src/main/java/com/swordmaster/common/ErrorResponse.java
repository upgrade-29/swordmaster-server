package com.swordmaster.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)      // code가 없으면 응답에서 생략
public class ErrorResponse {
    private final String message;
    private final String code;      // 클라이언트가 이유에 따라 다르게 처리해야 할 때만 사용 (예: ALREADY_IN_PROGRESS)

    public ErrorResponse(String message) {
        this(message, null);
    }

    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code    = code;
    }
}
