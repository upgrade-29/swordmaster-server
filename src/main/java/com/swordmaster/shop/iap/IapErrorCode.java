package com.swordmaster.shop.iap;

import com.swordmaster.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IapErrorCode implements ErrorCode {
    NOT_CASH_PRODUCT       (HttpStatus.BAD_REQUEST,         "현금 결제 상품이 아닙니다."),
    INVALID_RECEIPT        (HttpStatus.BAD_REQUEST,         "유효하지 않은 결제 정보입니다."),
    PURCHASE_PENDING       (HttpStatus.ACCEPTED,            "결제 승인 대기 중입니다. 잠시 후 다시 확인해 주세요."),
    RECEIPT_OWNER_MISMATCH (HttpStatus.FORBIDDEN,           "다른 계정의 결제 정보입니다."),
    STORE_UNAVAILABLE      (HttpStatus.SERVICE_UNAVAILABLE, "결제 확인이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),
    IAP_DISABLED           (HttpStatus.SERVICE_UNAVAILABLE, "결제 기능이 비활성화된 환경입니다.");

    private final HttpStatus status;
    private final String     message;
}