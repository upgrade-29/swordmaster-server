package com.swordmaster.ranking.redis;

import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingRedisListener {
    private final RankingRepository rankingRepository;
    private final RankingRedisStore rankingRedisStore;

    // 이벤트를 발행한 트랜잭션이 커밋된 후에 자동 실행 (발행 시점에는 대기, 롤백되면 실행되지 않음)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 발행한 트랜잭션에 합류하지 않고, 새 트랜잭션 생성 후 실행
    // 커밋이 완료됐다 해서 트랜잭션이 종료되는건 아님(아직 정리되지 않고 남아있음)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRecorded(RankingRecordedEvent event) {
        Long        playerId = event.playerId();
        RankingType type     = event.type();

        try {
            // 넘겨받은 점수가 아닌, DB의 최신 기록을 그대로 반영 (비교는 DB(upsert)에서 이미 수행)
            rankingRepository.findByPlayer_IdAndType(playerId, type)
                    .ifPresent(ranking -> rankingRedisStore.save(
                            ranking.getType(),
                            playerId,
                            ranking.getScore(),
                            ranking.getAchievedAt()
                    ));
        } catch (RuntimeException e) {      // Redis 오류(RedisConnectionFailureException)를 잡기 위함
            log.error("Redis 랭킹 반영 실패: playerId={}, type={}", playerId, type, e);
        }
    }
}
