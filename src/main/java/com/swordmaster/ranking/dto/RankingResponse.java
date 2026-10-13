package com.swordmaster.ranking.dto;

import java.util.List;

public record RankingResponse(
        List<RankingEntryResponse> rankings,
        RankingEntryResponse       me,
        long                       total
) {
}
