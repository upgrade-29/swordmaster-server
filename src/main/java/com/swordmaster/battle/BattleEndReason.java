package com.swordmaster.battle;

// 전투가 끝난 이유 (CLEARED만 승리)
public enum BattleEndReason {
    CLEARED,        // 마지막 세부 스테이지까지 승리
    PLAYER_DEAD,
    TIME_OVER,
    ABANDONED,      // 유저가 전투 포기
    VERIFY_FAILED   // 클라 보고가 서버 계산과 다르거나 너무 빠름
}
