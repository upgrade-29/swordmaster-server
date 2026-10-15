package com.swordmaster.ranking.service;

import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.dto.RankingEntryResponse;
import com.swordmaster.ranking.dto.RankingResponse;
import com.swordmaster.ranking.redis.RankingRecordedEvent;
import com.swordmaster.ranking.redis.RankingRedisStore;
import com.swordmaster.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RankingService {
    private final RankingRepository         rankingRepository;
    private final RankingRedisStore         rankingRedisStore;
    private final PlayerService             playerService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int RANKING_LIMIT = 100;

    // 조회
    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId, RankingType type) {
        Player               player = playerService.getMe(userId);
        RankingEntryResponse me     = getMe(player, type);      // 실시간 기록

        // 집계본에 현재 내 기록이 없고, 내 기록이 꼴찌일 경우에 인원수를 맞춤 (+ 1)
        long total = Math.max(rankingRedisStore.count(type), me.rank());

        return new RankingResponse(
                getTop(type, RANKING_LIMIT),            // 상위 N명 (집계본 기준)
                me,                                     // 내 랭킹 (실시간 기준)
                total,                                  // 총 인원 수 (클라이언트에서 내 랭킹(순위)의 백분율 계산용)
                rankingRedisStore.findUpdatedAt(type)   // 상위 목록 집계 시각
        );
    }

    // 조회 - 상위 N명
    private List<RankingEntryResponse> getTop(RankingType type, int size) {
        Map<Long, Long>   rankings  = rankingRedisStore.findTop(type, size);
        Map<Long, String> nicknames = playerService.getNicknames(rankings.keySet());

        // 정렬된 랭킹의 순서를 부여한 후 응답 반환
        List<RankingEntryResponse> result = new ArrayList<>();
        int                        rank    = 1;

        // Map 자체만으로는 반복자 사용 불가능 -> Map.Entry로 따로 변환 필요
        for (Map.Entry<Long, Long> ranking : rankings.entrySet()) {
            String nickname = nicknames.getOrDefault(ranking.getKey(), "알 수 없음");

            result.add(new RankingEntryResponse(
                    rank++,
                    nickname,
                    ranking.getValue()
            ));
        }

        return result;
    }

    // 조회 - 내 정보
    private RankingEntryResponse getMe(Player player, RankingType type) {
        // 내 순위를 조회 (없으면 null)
        Long   playerId = player.getId();
        String nickname = player.getNickname();
        Long   rank     = rankingRedisStore.findRank(type, playerId);

        // 기록이 없으면 빈 기록 반환
        if (rank == null) return RankingEntryResponse.none(nickname);

        // 내 순위 조회 후 반환
        long score = rankingRedisStore.findScore(type, playerId);

        return new RankingEntryResponse(Math.toIntExact(rank), nickname, score);
    }

    // 갱신 (or 생성)
    public void recordRanking(Player player, RankingType type, long score) {
        if (score <= 0) return;     // 0은 기록 없음으로 간주하고 기록하지 않음

        Long playerId = player.getId();

        rankingRepository.upsertIfHigher(
                playerId,
                type.name(),
                score,
                Instant.now()
        );

        // 커밋 후 redis에 반영(기록)하기 위해 이벤트 발행, 리스너가 캐치하고 이벤트 실행
        eventPublisher.publishEvent(new RankingRecordedEvent(playerId, type));
    }
}
