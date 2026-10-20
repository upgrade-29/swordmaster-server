package com.swordmaster.ranking.dto;

import java.time.Instant;
import java.util.List;

public record RankingResponse(
        List<RankingEntryResponse> rankings,    // 상위 목록 (집계본 기준)
        RankingEntryResponse       me,          // 자신의 기록 (실시간 기준)
        boolean                    rankChanged, // 집계 이후 내 기록이 바뀌었으면 true (클라이언트에서 "집계 전" 표시용)
        long                       total,       // 인원 수 (집계본 기준)
        Instant                    updatedAt,   // 상위 목록 집계 시각 (집계 전이면 null)
        Instant                    nextUpdateAt,// 다음 집계 예정 시각
        Instant                    serverTime   // 응답 생성 시각 (클라이언트 남은 시간 계산용)
) {
}
