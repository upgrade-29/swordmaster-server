package com.swordmaster.battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BattleEndResponse(
        int                     stage,
        @JsonProperty("isVictory")
        boolean                 isVictory,          // 포기, 검증 실패는 false
        BattleRewards           rewards,
        Map<CurrencyType, Long> currencies,         // 보상 반영 후 전체 재화 (PlayerResponse와 동일 형식)
        StageProgress           stageProgress,
        List<ArtifactResponse>  changedArtifacts,   // 없으면 빈 리스트
        Instant                 serverTime
) {
}
