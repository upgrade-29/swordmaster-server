package com.swordmaster.battle.session;

import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.BattleSessionInfo;
import com.swordmaster.battle.dto.CombatStat;

import java.time.Instant;

// Redis에 저장되는 진행 중인 전투 상태 (유저당 하나)
public record BattleSession(
        String     battleId,
        int        stage,
        long       battleSeed,          // 세부 스테이지 시드 = battleSeed + subStageIndex
        CombatStat playerStat,          // 전투 시작 시점의 스탯 스냅샷 (전투 중 강화/장착 변경 무시)
        int        subStageIndex,       // 지금 진행할 세부 스테이지
        double     playerHp,            // 이 세부 스테이지를 시작할 때의 체력
        long       accumulatedGold,
        String     gameDataVersion,     // 전투를 시작할 때의 게임 데이터 버전
        Instant    startedAt,
        Instant    subStageStartedAt,   // 너무 빠른 보고 검증용
        LastReport lastReport           // 직전 보고 (없으면 null)
) {
    // 같은 보고가 다시 오면 처음 응답을 그대로 반환하기 위해 보관
    public record LastReport(
            int                        subStageIndex,
            BattleResultVerifyResponse response
    ) {
    }

    // 새 전투
    public static BattleSession start(
            String     battleId,
            int        stage,
            long       battleSeed,
            CombatStat playerStat,
            String     gameDataVersion,
            Instant    now) {
        return new BattleSession(
                battleId, stage, battleSeed, playerStat,
                0, playerStat.maxHp(), 0,
                gameDataVersion, now, now, null
        );
    }

    // 다음 세부 스테이지로 진행
    public BattleSession advance(double nextPlayerHp, long nextAccumulatedGold,
                                 Instant now, BattleResultVerifyResponse response) {
        return new BattleSession(
                battleId, stage, battleSeed, playerStat,
                subStageIndex + 1, nextPlayerHp, nextAccumulatedGold,
                gameDataVersion, startedAt, now,
                new LastReport(subStageIndex, response)
        );
    }

    public BattleSessionInfo toInfo(Instant now) {
        return new BattleSessionInfo(
                battleId, stage, battleSeed, playerStat,
                subStageIndex, playerHp, accumulatedGold, now
        );
    }
}
