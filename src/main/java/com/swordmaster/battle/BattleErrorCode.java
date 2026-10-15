package com.swordmaster.battle;

import com.swordmaster.common.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 전투 요청을 거절한 이유 (ErrorResponse.code, 클라 BattleRequestErrorEnum과 같은 이름)
@Getter
@RequiredArgsConstructor
public enum BattleErrorCode {
    ALREADY_IN_PROGRESS(HttpStatus.CONFLICT,    "이미 진행 중인 전투가 있습니다."),
    STAGE_LOCKED       (HttpStatus.BAD_REQUEST, "아직 열리지 않은 스테이지입니다."),
    NO_ACTIVE_BATTLE   (HttpStatus.NOT_FOUND,   "진행 중인 전투가 없습니다."),
    BATTLE_ID_MISMATCH (HttpStatus.CONFLICT,    "현재 진행 중인 전투가 아닙니다."),
    VERIFY_FAILED      (HttpStatus.BAD_REQUEST, "전투 결과 검증에 실패했습니다."),     // 서버는 이미 패배로 정산한 상태
    GAME_DATA_OUTDATED (HttpStatus.CONFLICT,    "게임 데이터가 갱신되었습니다. 다시 접속해 주세요."),  // 세션은 유지
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT,    "이전 전투 요청을 처리 중입니다.");     // 같은 유저의 다른 전투 요청을 아직 처리 중

    private final HttpStatus status;
    private final String     message;

    public BusinessException exception() {
        return new BusinessException(status, name(), message);
    }
}
