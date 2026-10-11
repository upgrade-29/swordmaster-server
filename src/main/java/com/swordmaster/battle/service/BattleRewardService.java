package com.swordmaster.battle.service;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleRewards;
import com.swordmaster.battle.dto.StageProgress;
import com.swordmaster.battle.entity.BattleHistory;
import com.swordmaster.battle.entity.PlayerStageProgress;
import com.swordmaster.battle.repository.BattleHistoryRepository;
import com.swordmaster.battle.repository.PlayerStageProgressRepository;
import com.swordmaster.battle.session.BattleSession;
import com.swordmaster.battle.table.DropRate;
import com.swordmaster.battle.table.DropRateTable;
import com.swordmaster.battle.table.Stage;
import com.swordmaster.battle.table.StageTable;
import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.player.dto.PlayerArtifactResponse;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerArtifact;
import com.swordmaster.player.repository.PlayerArtifactRepository;
import com.swordmaster.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

// 전투 종료 시 DB 반영 (골드 지급, 스테이지 클리어, 아티팩트 드랍, 전투 기록)
// BattleService와 다른 빈이어야 트랜잭션이 적용되고, 검증 실패처럼 정산 후 예외를 던지는 경우에도 정산은 커밋됨
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BattleRewardService {
    private final PlayerRepository              playerRepository;
    private final PlayerArtifactRepository      playerArtifactRepository;
    private final PlayerStageProgressRepository stageProgressRepository;
    private final BattleHistoryRepository       battleHistoryRepository;
    private final CurrencyService               currencyService;
    private final StageTable                    stageTable;
    private final DropRateTable                 dropRateTable;
    private final ArtifactTable                 artifactTable;

    // gold: 지급할 골드 (마지막 처치 골드까지 포함한 최종값)
    public BattleEndResponse settle(
            Long            userId,
            BattleSession   session,
            BattleEndReason reason,
            int             lastSubStageIndex,
            long            gold,
            Instant         now) {
        Player                        player   = playerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "플레이어 정보가 없습니다."));
        boolean                       victory  = reason == BattleEndReason.CLEARED;
        Optional<PlayerStageProgress> progress = stageProgressRepository.findById(player.getId());
        int                           cleared  = progress.map(PlayerStageProgress::getClearedStage).orElse(0);

        // 이미 정산한 전투 (정산 후 Redis 세션 삭제에 실패한 경우), 보상 없이 현재 상태만 반환
        if (battleHistoryRepository.existsByBattleId(session.battleId())) {
            log.warn("이미 정산한 전투입니다: battleId={}, userId={}", session.battleId(), userId);

            return createResponse(session.stage(), victory, gold, List.of(), player, cleared, List.of(), now);
        }

        if (gold > 0) currencyService.grant(player, CurrencyType.GOLD, gold, CurrencyReason.REWARD);

        List<PlayerArtifact> changed      = List.of();
        List<String>         droppedCodes = List.of();

        if (victory) {
            // 처음 클리어하면 행 생성 (같은 유저의 정산은 BattleLock으로 하나씩 실행)
            if (progress.isPresent()) progress.get().clear(session.stage());
            else                      stageProgressRepository.save(new PlayerStageProgress(player.getId(), session.stage()));

            cleared = Math.max(cleared, session.stage());

            Optional<PlayerArtifact> dropped = dropArtifact(player, session.stage());

            if (dropped.isPresent()) {
                changed      = List.of(dropped.get());
                droppedCodes = List.of(dropped.get().getCode());
            }
        }

        battleHistoryRepository.save(new BattleHistory(
                session.battleId(),
                player.getId(),
                session.stage(),
                reason,
                lastSubStageIndex,
                gold,
                droppedCodes.isEmpty() ? null : String.join(",", droppedCodes),
                session.gameDataVersion(),
                session.startedAt()
        ));

        return createResponse(session.stage(), victory, gold, droppedCodes, player, cleared, changed, now);
    }

    // 승리 시 스테이지의 드랍 확률로 아티팩트 1개 드랍 (등급은 DropRateTable, 같은 등급 안에서는 균등)
    // 난수는 서버에서만 뽑음 (전투 시드로 뽑으면 클라가 드랍 결과를 미리 알 수 있음)
    private Optional<PlayerArtifact> dropArtifact(Player player, int stageNumber) {
        Stage             stage  = stageTable.findByStage(stageNumber)
                .orElseThrow(() -> new IllegalStateException("스테이지 정보가 없습니다: " + stageNumber));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() >= stage.artifactDropRate()) return Optional.empty();

        EquipmentGrade grade      = pickGrade(random.nextDouble());
        List<Artifact> candidates = artifactTable.getAll().stream()
                .filter(artifact -> artifact.grade() == grade)
                .toList();

        if (candidates.isEmpty()) {
            log.warn("{} 등급의 아티팩트가 없어 드랍하지 않습니다: stage={}", grade, stageNumber);

            return Optional.empty();
        }

        Artifact artifact = candidates.get(random.nextInt(candidates.size()));

        // 이미 가진 아티팩트여도 새로 추가 (인벤토리는 같은 아티팩트를 여러 개 가질 수 있음)
        return Optional.of(playerArtifactRepository.save(new PlayerArtifact(player, artifact.code())));
    }

    // 누적 확률로 등급 선택 (value: 0 이상 1 미만)
    private EquipmentGrade pickGrade(double value) {
        List<DropRate> rates      = dropRateTable.getAll();
        double         cumulative = 0;

        for (DropRate rate : rates) {
            cumulative += rate.rate();

            if (value < cumulative) return rate.grade();
        }

        return rates.getLast().grade();     // 합이 1보다 미세하게 작을 때
    }

    private BattleEndResponse createResponse(
            int                  stage,
            boolean              victory,
            long                 gold,
            List<String>         droppedCodes,
            Player               player,
            int                  clearedStage,
            List<PlayerArtifact> changed,
            Instant              now) {
        return new BattleEndResponse(
                stage,
                victory,
                new BattleRewards(gold, droppedCodes),
                currencyService.getAll(player),
                new StageProgress(clearedStage),
                changed.stream().map(PlayerArtifactResponse::from).toList(),
                now
        );
    }
}
