package com.swordmaster.battle.session;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;

// 끝난 전투의 결과 (battle:end:{battleId})
// 응답을 못 받은 클라가 같은 보고 / 포기를 다시 보내면 다시 정산하지 않고 처음과 같은 결과를 반환하기 위해 보관
public record BattleEndResult(
        BattleEndReason            reason,
        int                        subStageIndex,  // 전투가 끝난 세부 스테이지
        BattleEndResponse          battleEnd,
        BattleResultVerifyResponse report          // 보고로 끝났을 때의 응답 (포기, 검증 실패는 null)
) {
}
