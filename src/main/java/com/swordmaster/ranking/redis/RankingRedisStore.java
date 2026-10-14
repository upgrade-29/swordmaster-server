package com.swordmaster.ranking.redis;

import com.swordmaster.ranking.RankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// redis의 기록 저장 가능 기간을 넘어갈 경우의 추가 작업이 필요 (ex_ 시즌제 등)
@Component
@RequiredArgsConstructor
public class RankingRedisStore {
    private final StringRedisTemplate redis;

    // 점수(기록 + 달성 시간)를 합치기 위한 계산식 (순위 계산용, 달성 시간 = 달성 시각 - 기준 시각)
    // 순위 정렬에서 기록(과 합산)은 내림차순이지만 달성 시간은 오름차순이므로, '남은 시간 = 시각 자릿수의 최댓값 - 달성 시간' 으로 변환하여 내림차순 정렬
    // 합산 = 기록 x TIME_SLOT + 시각 (기록 비교 우선, 이후 시각 비교)
    // ex_ [기록 (6자리, 약 90만)] [시각 (10자리, 단위: 10ms, 약 3년치 저장 가능)] -> double 정밀도(약 9 x 10^15)
    //               12  9,999,998,999 = 12 x 10,000,000,000 + 9,999,998,999 (시각 자릿수의 최댓값: 9,999,999,999 = TIME_SLOT - 1)
    private static final long TIME_SLOT = 10_000_000_000L;                                             // 10ms(0.01초) 단위 시각이 들어갈 자리 (10자리)
    private static final long BASE_TIME = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();   // 기준 시각
    private static final long TIME_UNIT = 10;       // 단위 (10ms)

    // ==============================================================================================
    // 1. 실시간 랭킹 (Key = ranking:{type})
    // ==============================================================================================

    // 저장 (같은 플레이어면 점수만 변경)
    public void save(RankingType type, Long playerId, long score, Instant achievedAt) {
        // redis의 Sorted Set -> Score에 따라 '오름차순'으로 정렬된 Map<Value, Score> (조회 시 내림차순(Reverse)으로 반환 필요)
        redis.opsForZSet()
                .add(
                        key(type),                          // redis 내부에 각 key(랭킹 타입)에 따른 Sorted Set 생성
                        playerId.toString(),                // 각 Sorted Set의 멤버 (Value)
                        toRedisScore(score, achievedAt)     // 각 Sorted Set의 정렬 기준 (Score)
                );
    }

    // 저장 - 전체 (서버 시작 시 복구용)
    // TypedTuple<T>: Value<T> + Score<Double> 튜플 (Score 자료형은 Double)
    public void saveAll(RankingType type, Set<TypedTuple<String>> tuples) {
        if (tuples.isEmpty()) return;

        redis.opsForZSet()
                .add(
                        key(type),
                        tuples      // Value<String> + Score<Double> 튜플
                );
    }

    // 조회 - 내 기록 (기록이 있을 경우(rank is not null)에 호출)
    public long findScore(RankingType type, Long playerId) {
        // 리스트에서 자신의 기록 반환
        Double score = redis.opsForZSet()
                .score(
                        key(type),
                        playerId.toString()
                );

        return (score == null) ? 0 : toScore(score);    // 혹시나 기록이 없을 경우, 0 반환
    }

    // 조회 - 인원 수 (복구 여부 판단용)
    public long countLive(RankingType type) { return count(key(type)); }


    // ==============================================================================================
    // 2. 집계본 (Key = ranking:{type}:snapshot)
    // ==============================================================================================

    // 집계 - 실시간 랭킹을 집계본으로 복사하고 집계 시각 기록
    public void snapshot(RankingType type) {
        // 실시간 랭킹이 비어 있으면 집계본도 지움 (Copy는 원본이 없으면 아무것도 안함)
        if (count(key(type)) == 0) redis.delete(snapshotKey(type));
        // redis 안에서 통째로 복사 (덮어쓰기)
        else redis.copy(key(type), snapshotKey(type), true);

        redis.opsForValue()
                .set(
                        updatedAtKey(type),
                        String.valueOf(Instant.now().toEpochMilli())
                );
    }

    // 조회 - 상위 N명
    public Map<Long, Long> findTop(RankingType type, int size) {
        // 랭킹 타입(Key)에 따른 집계본(Sorted Set)을 '내림차순(Reverse)'으로 지정된 범위만큼 추출
        Set<TypedTuple<String>> tuples = redis.opsForZSet()
                .reverseRangeWithScores(
                        snapshotKey(type),      // Key는 집계본
                        0,
                        size - 1
                );

        Map<Long, Long> result = new LinkedHashMap<>();     // 넣는 순서대로 유지됨 (일반 HashMap은 섞임)

        // redis에서 꺼낸 값이 없으면 빈 Map 반환
        if (tuples == null) return result;

        for (TypedTuple<String> tuple : tuples) {
            Long playerId = Long.valueOf(Objects.requireNonNull(tuple.getValue()));
            Long score    = toScore(Objects.requireNonNull(tuple.getScore()));      // 합산(기록 + 시간)에서 원래 기록을 추출

            result.put(playerId, score);
        }

        return result;
    }

    // 조회 - 내 예상 순위 (자신의 실시간 기록을 집계본에 대조할 때의 예상 순위, 기록이 없으면 null)
    public Long findRank(RankingType type, Long playerId) {
        // 실시간 랭킹의 자신의 합산 점수
        Double myScore = redis.opsForZSet()
                .score(
                        key(type),
                        playerId.toString()
                );

        if (myScore == null) return null;

        // 집계본에서 내 점수보다 높은 사람의 수 (점수 기준 오름차순)
        Long ahead = redis.opsForZSet()
                .count(
                        snapshotKey(type),
                        Math.nextUp(myScore),
                        Double.POSITIVE_INFINITY
                );

        // 오름차순으로 정렬된 기록에서 자신보다 뒤에 있는 기록이 없으면(자신의 점수가 가장 높으면) 1위
        return ((ahead == null) ? 0 : ahead) + 1;
    }

    // 조회 - 전체 인원 수
    public long count(RankingType type) { return count(snapshotKey(type)); }

    // 조회 - 마지막 집계 시각 (집계 전이면 null)
    public Instant findUpdatedAt(RankingType type) {
        String millis = redis.opsForValue().get(updatedAtKey(type));

        return (millis == null) ? null : Instant.ofEpochMilli(Long.parseLong(millis));
    }


    // ==============================================================================================
    // 3. 공통
    // ==============================================================================================

    // 변환 - 복구용
    public static TypedTuple<String> toTuple(Long playerId, long score, Instant achievedAt) {
        return TypedTuple.of(
                playerId.toString(),
                toRedisScore(score, achievedAt)     // 기록 + 시간의 합산
        );
    }

    // Key 생성 (랭킹 종류(type -> key) 마다 Sorted Set 생성)
    private String key(RankingType type)          { return "ranking:" + type.name(); }
    private String snapshotKey(RankingType type)  { return key(type) + ":snapshot"; }
    private String updatedAtKey(RankingType type) { return snapshotKey(type) + ":updated-at"; }

    // Sorted Set 인원 수
    private long count(String key) {
        Long count = redis.opsForZSet().zCard(key);

        return (count == null) ? 0 : count;
    }

    // 합산 (기록 + 시간)
    private static double toRedisScore(long score, Instant achievedAt) {
        // 경과 시간 = (기록 시각 - 기준 시각) / 단위
        long elapsed = (achievedAt.toEpochMilli() - BASE_TIME) / TIME_UNIT;

        if (elapsed < 0 || elapsed >= TIME_SLOT)
            throw new IllegalStateException("기간을 벗어난 기록입니다: " + achievedAt);

        return score * (double) TIME_SLOT + (TIME_SLOT - 1 - elapsed);  // 기록 x TIME_SLOT (단위) + 남은 시간 (자릿수의 최댓 값 - 경과 시간)
    }

    // 기록 추출 (합산 = 기록 + 시간 에서 시간(자릿수)을 날려 본래의 기록 반환)
    private static long toScore(double redisScore) { return (long) (redisScore / TIME_SLOT); }
}
