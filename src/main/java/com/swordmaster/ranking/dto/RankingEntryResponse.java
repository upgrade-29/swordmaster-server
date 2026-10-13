package com.swordmaster.ranking.dto;

import com.swordmaster.ranking.entity.PlayerRanking;

public record RankingEntryResponse(int rank, String nickname, long score) {
    public static RankingEntryResponse of(int rank, PlayerRanking ranking) {
        return new RankingEntryResponse(
                rank,
                ranking.getPlayer().getNickname(),
                ranking.getScore()
        );
    }

    public static RankingEntryResponse none(String nickname) {
        return new RankingEntryResponse(0, nickname, 0);
    }
}
