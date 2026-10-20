package com.swordmaster.ranking.service;

import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.service.CombatStatCalculator;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.repository.ArtifactRepository;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.dto.RankingEntryResponse;
import com.swordmaster.ranking.dto.RankingResponse;
import com.swordmaster.ranking.redis.RankingRecordedEvent;
import com.swordmaster.ranking.redis.RankingRedisStore;
import com.swordmaster.ranking.redis.RankingSnapshotJob;
import com.swordmaster.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
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

    // 전투력 계산용
    private final ArtifactRepository   artifactRepository;
    private final CombatStatCalculator combatStatCalculator;
    private final ConstantTable        constantTable;

    private static final int RANKING_LIMIT = 100;

    // 다음 집계 시각 계산용 (집계 스케줄러와 같은 cron, 시간대 사용)
    private static final CronExpression SNAPSHOT_CRON = CronExpression.parse(RankingSnapshotJob.SNAPSHOT_CRON);
    private static final ZoneId         SNAPSHOT_ZONE = ZoneId.of(RankingSnapshotJob.SNAPSHOT_ZONE);

    // 조회
    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId, RankingType type) {
        Player               player = playerService.getMe(userId);
        RankingEntryResponse me     = getMe(player, type);      // 실시간 기록

        Long    playerId    = player.getId();
        boolean inSnapshot  = rankingRedisStore.existsInSnapshot(type, playerId);
        boolean rankChanged = rankingRedisStore.isChangedSinceSnapshot(type, playerId);

        // 실시간 기록은 있는데 집계본에 내 기록이 없으면, 순위 비교 대상에 나를 포함하도록 인원수를 맞춤 (+ 1)
        boolean hasRecord = me.rank() > 0;
        long    total     = rankingRedisStore.count(type) + ((hasRecord && !inSnapshot) ? 1 : 0);

        Instant now          = Instant.now();
        Instant nextUpdateAt = SNAPSHOT_CRON.next(now.atZone(SNAPSHOT_ZONE)).toInstant();

        return new RankingResponse(
                getTop(type),                           // 상위 N명 (집계본 기준)
                me,                                     // 내 랭킹 (실시간 기준)
                rankChanged,                            // 집계 이후 내 기록 변동 여부
                total,                                  // 총 인원 수 (클라이언트에서 내 랭킹(순위)의 백분율 계산용)
                rankingRedisStore.findUpdatedAt(type),  // 상위 목록 집계 시각
                nextUpdateAt,                           // 다음 집계 시각
                now                                     // 서버 현재 시각
        );
    }

    // 조회 - 상위 N명
    private List<RankingEntryResponse> getTop(RankingType type) {
        Map<Long, Long>   rankings  = rankingRedisStore.findTop(type, RANKING_LIMIT);
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

    // 갱신 (전투력 한정)
    public void recordCombatPower(Player player) {
        int                    swordLevel = player.getSwordLevel();
        List<ArtifactResponse> equipped   = artifactRepository.findAllByPlayerAndEquipSlotIsNotNull(player)
                .stream()
                .map(ArtifactResponse::from)
                .toList();

        // 전투력 계산 (임시)
        CombatStat stat           = combatStatCalculator.calculate(swordLevel, equipped);
        double     critMultiplier = constantTable.getDecimal(ConstantKey.CRIT_MULTIPLIER);
        double     baseBattleTime = constantTable.getLong(ConstantKey.BASE_BATTLE_TIME);
        double     expectedDps    = stat.attack() * stat.attackSpeed() * (1 + stat.critRate() * (critMultiplier - 1));
        double     effectiveHp    = stat.maxHp() + expectedDps * stat.lifesteal() * baseBattleTime;
        long       score          = (long) Math.floor(Math.sqrt(expectedDps * effectiveHp) * 10);

        Long        playerId = player.getId();
        RankingType type     = RankingType.COMBAT_POWER;

        rankingRepository.upsertCurrent(
                playerId,
                type.name(),
                score,
                Instant.now()
        );

        eventPublisher.publishEvent(new RankingRecordedEvent(playerId, type));
    }
}
