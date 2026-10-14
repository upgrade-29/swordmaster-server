package com.swordmaster.ranking.dto;

public record RankingEntryResponse(int rank, String nickname, long score) {
    public static RankingEntryResponse none(String nickname) {
        return new RankingEntryResponse(0, nickname, 0);
    }
}
