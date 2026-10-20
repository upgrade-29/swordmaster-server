package com.swordmaster.ranking.dto;

import java.time.Instant;
import java.util.List;

public record RankingResponse(
        List<RankingEntryResponse> rankings,    // 상위 목록 (집계본 기준)
        RankingEntryResponse       me,          // 자신의 기록 (실시간 기준)
        long                       total,       // 인원 수 (집계본 기준)
        Instant                    updatedAt,   // 상위 목록 집계 시각 (집계 전이면 null)
        Instant                    nextUpdateAt,// 다음 집계 예정 시각
        Instant                    serverTime   // 응답 생성 시각 (클라이언트 남은 시간 계산용)
) {
}
