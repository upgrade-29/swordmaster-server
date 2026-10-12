package com.swordmaster.equipment;

import com.swordmaster.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EquipmentErrorCode implements ErrorCode {
    LEVEL_MISMATCH      (HttpStatus.BAD_REQUEST, "장비의 레벨이 서버와 일치하지 않습니다."),
    SWORD_MAX_LEVEL     (HttpStatus.BAD_REQUEST, "이미 최고 레벨에 도달했습니다."),
    SWORD_NOT_SELLABLE  (HttpStatus.BAD_REQUEST, "검을 판매할 수 없습니다."),
    ARTIFACT_NOT_OWNED  (HttpStatus.NOT_FOUND,   "아티팩트를 소유하고 있지 않습니다."),
    ARTIFACT_MAX_LEVEL  (HttpStatus.BAD_REQUEST, "이미 최고 레벨에 도달했습니다."),
    NOT_ENOUGH_MATERIAL (HttpStatus.BAD_REQUEST, "강화 재료가 부족합니다."),
    INVALID_SLOT        (HttpStatus.BAD_REQUEST, "유효하지 않은 슬롯입니다.");

    private final HttpStatus status;
    private final String     message;
}
