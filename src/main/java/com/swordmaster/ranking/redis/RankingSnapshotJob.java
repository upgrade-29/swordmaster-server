package com.swordmaster.ranking.redis;

import com.swordmaster.ranking.RankingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 정해진 주기마다 실시간 랭킹을 집계본으로 복사 (상위 랭킹 갱신)
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingSnapshotJob {
    private final RankingRedisStore rankingRedisStore;

    // 자바에서는 컴파일 시점에 상수가 정해지므로, 상수 정의에 다른 상수를 이어 붙일 수 있음
    // SNAPSHOT_CRON, SNAPSHOT_ZONE은 랭킹 조회 응답의 다음 집계 시각 계산에도 사용 (RankingService)
    private static final int    SNAPSHOT_HOUR = 1;
//    public  static final String SNAPSHOT_CRON = "0 0 " + SNAPSHOT_HOUR + " * * *";
    public  static final String SNAPSHOT_ZONE = "Asia/Seoul";
    public static final String SNAPSHOT_CRON = "0 * * * * *";  // 임시, 매 분마다

    // 마찬가지로, 컴파일에 정의된 상수를 어노테이션에 사용 가능
    // AWS 서버에 올릴 경우 기본 시간은 UTC 기준 -> 특정 지역의 시간으로 변환
    @Scheduled(cron = SNAPSHOT_CRON, zone = SNAPSHOT_ZONE)
    public void snapshot() {
        for (RankingType type : RankingType.values()) {
            try {
                rankingRedisStore.snapshot(type);
            } catch (RuntimeException e) {
                // 한 랭킹 타입이 실패해도 나머지 타입은 계속 집계, 실패한 타입은 다음 주기에 시도
                log.error("랭킹 집계 실패: type={}", type, e);
            }
        }
    }
}
