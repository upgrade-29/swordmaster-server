package com.swordmaster.ranking.redis;

import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.entity.PlayerRanking;
import com.swordmaster.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 서버 실행시 redis 실시간 랭킹이 비어있으면 db 원본으로 다시 복구
// 집계본이 없으면 한 번 집계
@Component
@RequiredArgsConstructor
public class RankingRedisRestorer implements ApplicationRunner {
    private final RankingRepository rankingRepository;
    private final RankingRedisStore rankingRedisStore;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        // 각 랭킹 타입에 따라
        for (RankingType type : RankingType.values()) {
            // 실시간 랭킹이 비어 있으면 DB 원본으로 복구
            if (rankingRedisStore.countLive(type) == 0) {
                List<PlayerRanking> rankings = rankingRepository.findAllByType(type);

                Set<TypedTuple<String>> tuples = rankings.stream()
                        .map(ranking -> RankingRedisStore.toTuple(
                                ranking.getPlayer().getId(),
                                ranking.getScore(),
                                ranking.getAchievedAt()
                        ))
                        .collect(Collectors.toSet());

                rankingRedisStore.saveAll(type, tuples);
            }

            // 집계본이 한 번도 기록되지 않았을 경우, 바로 한 번 집계
            if (rankingRedisStore.findUpdatedAt(type) == null) rankingRedisStore.snapshot(type);
        }
    }
}
