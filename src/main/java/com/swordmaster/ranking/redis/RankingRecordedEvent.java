package com.swordmaster.ranking.redis;

import com.swordmaster.ranking.RankingType;

// 기록(랭킹)이 저장됐음을 알리는 이벤트 (해당 이벤트가 발행되면 리스너가 캐치)
public record RankingRecordedEvent(Long playerId, RankingType type) {
}