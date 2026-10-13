package com.swordmaster.currency;

import com.swordmaster.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CurrencyErrorCode implements ErrorCode {
    NOT_ENOUGH_GOLD    (HttpStatus.BAD_REQUEST, "골드가 부족합니다."),
    NOT_ENOUGH_DIAMOND (HttpStatus.BAD_REQUEST, "다이아가 부족합니다.");

    private final HttpStatus status;
    private final String     message;
}
