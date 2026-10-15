package com.swordmaster.battle.service;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.BattleErrorCode;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleResultVerifyRequest;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.BattleSessionInfo;
import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.session.BattleEndResult;
import com.swordmaster.battle.session.BattleLock;
import com.swordmaster.battle.session.BattleSession;
import com.swordmaster.battle.session.BattleSessionRepository;
import com.swordmaster.battle.simulation.BattleSimulator;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.battle.simulation.SubStageResult;
import com.swordmaster.battle.table.BattleConfig;
import com.swordmaster.battle.table.Enemy;
import com.swordmaster.battle.table.EnemyTable;
import com.swordmaster.battle.table.Stage;
import com.swordmaster.battle.table.StageTable;
import com.swordmaster.common.table.GameDataVersion;
import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.service.PlayerFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// 전투 흐름 (Redis 세션 + 세부 스테이지 검증)
// 같은 유저의 요청은 BattleLock으로 하나씩 처리, DB 반영은 BattleRewardService(트랜잭션)에 맡김
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {
    private static final int SETTLE_MAX_RETRY = 2;     // 재화 동시 변경(낙관적 락) 충돌 시 재시도 (총 시도 횟수는 3)

    private final BattleSessionRepository       sessionRepository;
    private final BattleLock                    battleLock;
    private final BattleRewardService           battleRewardService;
    private final CombatStatCalculator          combatStatCalculator;
    private final PlayerFacade                  playerFacade;
    private final StageTable                    stageTable;
    private final EnemyTable                    enemyTable;
    private final BattleConfig                  battleConfig;
    private final GameDataVersion               gameDataVersion;
    private final Clock                         clock;

    private final SecureRandom seedRandom = new SecureRandom();     // 시드 발급용 (클라가 다음 시드를 예측할 수 없도록)

    // 진행 중인 세션 조회 (없으면 null)
    public BattleSessionInfo getSession(Long userId, String clientGameDataVersion) {
        checkGameDataVersion(clientGameDataVersion);

        return battleLock.execute(userId, () -> {
            Optional<BattleSession> found = sessionRepository.find(userId);

            if (found.isEmpty()) return null;

            Instant       now     = clock.instant();
            BattleSession session = found.get().restartSubStage(now);

            sessionRepository.save(userId, session);

            return session.toInfo(now);
        });
    }

    // 전투 시작
    public BattleSessionInfo start(Long userId, int stageNumber, String clientGameDataVersion) {
        checkGameDataVersion(clientGameDataVersion);

        return battleLock.execute(userId, () -> {
            if (sessionRepository.find(userId).isPresent()) throw BattleErrorCode.ALREADY_IN_PROGRESS.exception();

            PlayerResponse player = playerFacade.getMe(userId);

            // 이미 깬 스테이지는 다시 도전할 수 있고, 아직 열리지 않은 스테이지는 도전할 수 없음
            if (stageTable.findByStage(stageNumber).isEmpty() || stageNumber > player.stageProgress().clearedStage() + 1)
                throw BattleErrorCode.STAGE_LOCKED.exception();

            CombatStat    playerStat = combatStatCalculator.calculate(player.swordResponse().level(), player.artifactResponses());
            Instant       now        = clock.instant();
            BattleSession session    = BattleSession.start(
                    UUID.randomUUID().toString().replace("-", ""),
                    stageNumber,
                    seedRandom.nextLong() & Long.MAX_VALUE,     // 0 이상 (클라가 ulong)
                    playerStat,
                    clientGameDataVersion,
                    now
            );

            if (!sessionRepository.saveIfAbsent(userId, session)) throw BattleErrorCode.ALREADY_IN_PROGRESS.exception();

            return session.toInfo(now);
        });
    }

    // 세부 스테이지 결과 보고
    public BattleResultVerifyResponse report(Long userId, BattleResultVerifyRequest request, String clientGameDataVersion) {
        // 데이터가 다르면 검증할 수 없음, 유저 잘못이 아니므로 세션은 끝내지 않음
        checkGameDataVersion(clientGameDataVersion);

        return battleLock.execute(userId, () -> {
            Optional<BattleSession> current = sessionRepository.find(userId);
            Optional<BattleSession> found   = current.filter(session -> session.battleId().equals(request.battleId()));

            if (found.isPresent()) {
                // 이미 검증한 보고면 다시 검증하지 않고 처음 응답을 그대로 반환
                Optional<BattleResultVerifyResponse> previous = found.map(BattleSession::lastReport)
                        .filter(last -> last.subStageIndex() == request.subStageIndex())
                        .map(BattleSession.LastReport::response);

                return previous.orElseGet(() -> verifyAndApply(userId, found.get(), request));
            }

            // 이 보고로 이미 끝난 전투면 처음과 같은 결과 (검증 실패도 다시 정산하지 않고 같은 에러)
            Optional<BattleEndResult> ended = sessionRepository.findEndResult(request.battleId())
                    .filter(result -> result.subStageIndex() == request.subStageIndex());

            if (ended.isPresent()) {
                if (ended.get().reason() == BattleEndReason.VERIFY_FAILED) throw BattleErrorCode.VERIFY_FAILED.exception();
                if (ended.get().report() != null) return ended.get().report();
            }

            throw current.isPresent()
                    ? BattleErrorCode.BATTLE_ID_MISMATCH.exception()
                    : BattleErrorCode.NO_ACTIVE_BATTLE.exception();
        });
    }

    // 전투 포기 (패배 처리, 모은 골드는 지급)
    public BattleEndResponse abandon(Long userId, String battleId) {
        return battleLock.execute(userId, () -> {
            Optional<BattleSession> current = sessionRepository.find(userId);
            Optional<BattleSession> found   = current.filter(session -> session.battleId().equals(battleId));

            // 이미 포기한 전투면 다시 정산하지 않고 처음 응답을 그대로 반환
            if (found.isEmpty()) {
                return sessionRepository.findEndResult(battleId)
                        .filter(result -> result.reason() == BattleEndReason.ABANDONED)
                        .map(BattleEndResult::battleEnd)
                        .orElseThrow(() -> current.isPresent()
                                ? BattleErrorCode.BATTLE_ID_MISMATCH.exception()
                                : BattleErrorCode.NO_ACTIVE_BATTLE.exception());
            }

            BattleSession     session = found.get();
            BattleEndResponse end     = settle(userId, session, BattleEndReason.ABANDONED,
                    session.subStageIndex(), session.accumulatedGold());

            finish(userId, session, new BattleEndResult(BattleEndReason.ABANDONED, session.subStageIndex(), end, null));

            return end;
        });
    }

    // 같은 시드와 스탯으로 다시 돌려서 클라가 보낸 요약과 비교
    private BattleResultVerifyResponse verifyAndApply(Long userId, BattleSession session, BattleResultVerifyRequest request) {
        int            index    = session.subStageIndex();
        Stage          stage    = stageTable.findByStage(session.stage())
                .orElseThrow(() -> new IllegalStateException("스테이지 정보가 없습니다: " + session.stage()));
        Enemy          enemy    = enemyTable.findByCode(stage.enemyCodeAt(index))
                .orElseThrow(() -> new IllegalStateException("적 정보가 없습니다: " + stage.enemyCodeAt(index)));
        SubStageResult expected = battleConfig.getSimulator().simulate(
                session.playerStat(),
                session.playerHp(),
                BattleSimulator.enemyStat(enemy.baseStat(), stage.statMultiplier()),
                battleConfig.getTimeLimit(enemy),
                session.battleSeed(),
                index
        );

        Duration         elapsed    = Duration.between(session.subStageStartedAt(), clock.instant());
        Optional<String> failReason = BattleVerifier.verify(request, index, expected, elapsed);

        // 같은 세부 스테이지를 계속 다시 보내며 시험해 보지 못하게, 검증에 실패하면 패배로 끝냄
        if (failReason.isPresent()) {
            log.warn("전투 검증 실패: userId={}, battleId={}, subStage={}, reason={}",
                    userId, session.battleId(), index, failReason.get());

            BattleEndResponse end = settle(userId, session, BattleEndReason.VERIFY_FAILED, index, session.accumulatedGold());

            finish(userId, session, new BattleEndResult(BattleEndReason.VERIFY_FAILED, index, end, null));

            throw BattleErrorCode.VERIFY_FAILED.exception();
        }

        // 패배 (사망, 시간 초과)
        if (expected.resultEnum() != BattleSubStageResultEnum.ENEMY_DEAD) {
            BattleEndReason reason = expected.resultEnum() == BattleSubStageResultEnum.PLAYER_DEAD
                    ? BattleEndReason.PLAYER_DEAD
                    : BattleEndReason.TIME_OVER;

            return end(userId, session, expected, reason, session.accumulatedGold());
        }

        long gold = session.accumulatedGold() + stage.killGoldAt(index);

        // 마지막 세부 스테이지 승리
        if (index == stage.subStageCount() - 1) return end(userId, session, expected, BattleEndReason.CLEARED, gold);

        // 다음 세부 스테이지로 진행, 체력은 이어지고 처치 회복을 더함
        double                     nextHp   = expected.playerHp() + expected.healOnKill();
        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                index, expected.resultEnum(), index + 1, nextHp, gold, null
        );

        sessionRepository.save(userId, session.advance(nextHp, gold, clock.instant(), response));

        return response;
    }

    // 전투 종료, 응답은 같은 보고가 다시 와도 돌려줄 수 있도록 세션과 따로 보관
    private BattleResultVerifyResponse end(Long userId, BattleSession session, SubStageResult result,
                                           BattleEndReason reason, long gold) {
        int               index = session.subStageIndex();
        BattleEndResponse end   = settle(userId, session, reason, index, gold);

        // 전투가 끝났으면 처치 회복 없이 끝난 체력 그대로
        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                index, result.resultEnum(), index, result.playerHp(), gold, end
        );

        finish(userId, session, new BattleEndResult(reason, index, end, response));

        return response;
    }

    // 끝난 결과를 보관하고 세션 삭제
    private void finish(Long userId, BattleSession session, BattleEndResult result) {
        sessionRepository.saveEndResult(session.battleId(), result);
        sessionRepository.delete(userId);
    }

    private BattleEndResponse settle(Long userId, BattleSession session, BattleEndReason reason,
                                     int lastSubStageIndex, long gold) {
        return retryOnConflict(userId, () ->
                battleRewardService.settle(userId, session, reason, lastSubStageIndex, gold, clock.instant()));
    }

    // 상점 구매 등과 재화가 동시에 바뀌면 낙관적 락 충돌, 트랜잭션 전체를 다시 실행
    private <T> T retryOnConflict(Long userId, Supplier<T> action) {
        for (int retry = 0; ; retry++) {
            try {
                return action.get();
            } catch (ConcurrencyFailureException e) {
                if (retry >= SETTLE_MAX_RETRY) throw e;

                log.warn("전투 정산 동시성 충돌 재시도 {}/{}: userId={}", retry + 1, SETTLE_MAX_RETRY, userId);
            }
        }
    }

    private void checkGameDataVersion(String clientGameDataVersion) {
        if (!gameDataVersion.matches(clientGameDataVersion)) throw BattleErrorCode.GAME_DATA_OUTDATED.exception();
    }
}
