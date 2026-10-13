package com.swordmaster.ranking.service;

import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.dto.RankingEntryResponse;
import com.swordmaster.ranking.dto.RankingResponse;
import com.swordmaster.ranking.entity.PlayerRanking;
import com.swordmaster.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class RankingService {
    private final RankingRepository rankingRepository;
    private final PlayerService     playerService;

    private static final int RANKING_LIMIT = 100;

    // 조회
    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId, RankingType type) {
        Player player = playerService.getMe(userId);

        return new RankingResponse(
                getTop(type, RANKING_LIMIT),
                getMe(player, type),
                rankingRepository.countByType(type)
        );
    }

    // 조회 (상위 N명)
    private List<RankingEntryResponse> getTop(RankingType type, int size) {
        // 랭킹을 정렬해서 0번째 페이지 내용(size)만큼 조회
        List<PlayerRanking> rankings = rankingRepository.findTop(type, PageRequest.of(0, size));

        // 정렬된 랭킹의 순서(i + 1)를 추가로 부여한 후 응답 반환
        return IntStream.range(0, rankings.size())
                .mapToObj(index -> RankingEntryResponse.of(index + 1, rankings.get(index)))
                .toList();
    }

    // 조회 (내 정보)
    private RankingEntryResponse getMe(Player player, RankingType type) {
        Optional<PlayerRanking> found = rankingRepository.findByPlayerAndType(player, type);

        // 기록이 없으면 빈 기록 반환
        if (found.isEmpty()) return RankingEntryResponse.none(player.getNickname());

        // 내 순위 조회 후 반환
        PlayerRanking ranking = found.get();
        long          ahead   = rankingRepository.countAhead(
                type,
                ranking.getScore(),
                ranking.getAchievedAt(),
                ranking.getId()
        );

        int rank = Math.toIntExact(ahead + 1);      // 순위: index + 1

        return RankingEntryResponse.of(rank, ranking);
    }

    // 갱신 (or 생성)
    public void recordRanking(Player player, RankingType type, long score) {
        if (score <= 0) return;     // 0은 기록 없음으로 간주하고 기록하지 않음

        rankingRepository.upsertIfHigher(
                player.getId(),
                type.name(),
                score,
                Instant.now()
        );
    }
}
