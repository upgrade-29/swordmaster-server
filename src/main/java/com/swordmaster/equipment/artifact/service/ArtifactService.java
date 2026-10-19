package com.swordmaster.equipment.artifact.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.EquipmentErrorCode;
import com.swordmaster.equipment.artifact.dto.ArtifactEnhanceRequest;
import com.swordmaster.equipment.artifact.dto.ArtifactEnhanceResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.entity.PlayerArtifact;
import com.swordmaster.equipment.artifact.repository.ArtifactRepository;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactEnhance;
import com.swordmaster.equipment.artifact.table.ArtifactEnhanceTable;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtifactService {
    private final ArtifactTable        artifactTable;
    private final ArtifactEnhanceTable artifactEnhanceTable;
    private final PlayerService        playerService;
    private final ArtifactRepository   artifactRepository;
    private final CurrencyService      currencyService;
    private final IdempotencyService   idempotencyService;
    private final ConstantTable        constantTable;
    private final RankingService       rankingService;

    // 임시 설정 값
    private static final int          SLOT_COUNT    = 3;
    private static final CurrencyType CURRENCY_TYPE = CurrencyType.GOLD;

    // 생성
    public List<ArtifactResponse> createDefault(Player player) {
        List<PlayerArtifact> playerArtifacts = new ArrayList<>();
        List<String>         codeList        = constantTable.getTextList(ConstantKey.STARTER_ARTIFACTS);

        for (String code : codeList) {
            if (!artifactTable.existsByCode(code)) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "아티팩트를 생성할 수 없습니다.");

            playerArtifacts.add(new PlayerArtifact(player, code));
        }

        artifactRepository.saveAll(playerArtifacts);

        return playerArtifacts.stream()
                .map(ArtifactResponse::from)
                .toList();
    }

    // 조회
    public List<ArtifactResponse> getAll(Player player) {
        List<PlayerArtifact> playerArtifacts = artifactRepository.findAllByPlayer(player);

        return playerArtifacts.stream()
                .map(ArtifactResponse::from)
                .toList();
    }

    // 강화
    public ArtifactEnhanceResponse enhance(Long userId, String code, ArtifactEnhanceRequest request) {
        Player player = playerService.getMe(userId);

        return idempotencyService.execute(
                userId,
                request.requestId(),
                ArtifactEnhanceResponse.class,
                () -> doEnhance(player, code)
        );
    }

    private ArtifactEnhanceResponse doEnhance(Player player, String code) {
        // 아티팩트 소유 검사
        PlayerArtifact playerArtifact = find(player, code);
        Artifact       artifact       = artifactTable.findByCode(code)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "아티팩트를 강화할 수 없습니다."));

        // 최대 레벨 검사 (Enhance 테이블이 온전한 경우를 가정)
        int level = playerArtifact.getLevel();

        ArtifactEnhance artifactEnhance = artifactEnhanceTable.findByGradeAndLevel(artifact.grade(), level)
                .orElseThrow(() -> new BusinessException(EquipmentErrorCode.ARTIFACT_MAX_LEVEL));

        // 강화 재료 검사
        int materialCount = playerArtifact.getMaterialCount();
        int requiredCount = artifactEnhance.materialCount();

        if (materialCount < requiredCount) throw new BusinessException(EquipmentErrorCode.NOT_ENOUGH_MATERIAL);

        // 재화 소비 (+ 재화량 검사)
        long               cost               = artifactEnhance.gold();
        CurrenciesResponse currenciesResponse = currencyService.consume(player, CURRENCY_TYPE, cost, CurrencyReason.EQUIP_ENHANCE);

        // 강화
        playerArtifact.changeLevel(level + 1);
        playerArtifact.changeMaterialCount(materialCount - requiredCount);
        artifactRepository.save(playerArtifact);

        // 전투력 랭킹 기록
        rankingService.recordCombatPower(player);

        return new ArtifactEnhanceResponse(
                List.of(ArtifactResponse.from(playerArtifact)),
                currenciesResponse
        );
    }

    // 장착 (및 변경)
    public List<ArtifactResponse> equip(Long userId, String code, int slot) {
        // 슬롯 검사
        if (slot <= 0 || slot > SLOT_COUNT) throw new BusinessException(EquipmentErrorCode.INVALID_SLOT);

        Player player = playerService.getMe(userId);

        // 아티팩트 소유 검사
        PlayerArtifact required  = find(player, code);
        Integer        equipSlot = required.getEquipSlot();

        // 아티팩트가 이미 장착되어 있는 경우
        if (equipSlot != null) {
            // 이미 요청한 슬롯에 장착되어 있으면 변경 없이 반환
            if (equipSlot == slot) {
                return toSortedResponses(artifactRepository.findAllByPlayerAndEquipSlotIsNotNull(player));
            }

            // 장착되어 있는 아티팩트 해제
            doRemove(player, equipSlot);
        }

        // 요청할 슬롯에 다른 아티팩트가 장착되어 있으면 해제
        List<PlayerArtifact> equipped = doRemove(player, slot);

        // 요청한 아티팩트 장착
        required.changeEquipSlot(slot);
        equipped.add(required);
        artifactRepository.save(required);

        // 전투력 랭킹 기록
        rankingService.recordCombatPower(player);

        return toSortedResponses(equipped);
    }

    // 해제
    public List<ArtifactResponse> remove(Long userId, int slot) {
        // 슬롯 검사
        if (slot <= 0 || slot > SLOT_COUNT) throw new BusinessException(EquipmentErrorCode.INVALID_SLOT);

        Player               player   = playerService.getMe(userId);
        List<PlayerArtifact> equipped = doRemove(player, slot);

        // 전투력 랭킹 기록
        rankingService.recordCombatPower(player);

        return toSortedResponses(equipped);
    }

    // 해제 - 공통
    private List<PlayerArtifact> doRemove(Player player, int slot) {
        List<PlayerArtifact> equipped = artifactRepository.findAllByPlayerAndEquipSlotIsNotNull(player);

        equipped.removeIf(
                artifact -> {
                    if (artifact.getEquipSlot() != slot) return false;

                    artifact.changeEquipSlot(null);
                    artifactRepository.saveAndFlush(artifact);

                    return true;
                }
        );

        return equipped;
    }

    public PlayerArtifact grantMaterialCount(Player player,String code,long amount){
        //아티팩트 코드가 테이블에 존재하지 않는지와 수량이 0이거나 0보다 작은 경우 예외 (상태를 500으로 한 이유는 시트 데이터는 관리자가 설정했기에)
        if (!artifactTable.existsByCode(code))
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"NOT_EXIST_CODE: " + code);
        if (amount <= 0)
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"AMOUNT_IS_WRONG: " + amount);

        int count = Math.toIntExact(amount);

        PlayerArtifact playerArtifact = artifactRepository.findByPlayerAndCode(player,code)
                .orElseGet(() -> artifactRepository.save(new PlayerArtifact(player,code)));

        playerArtifact.changeMaterialCount(Math.addExact(playerArtifact.getMaterialCount(), count));

        return playerArtifact;
    }

    // 공통 (조회)
    private PlayerArtifact find(Player player, String code) {
        return artifactRepository.findByPlayerAndCode(player, code)
                .orElseThrow(() -> new BusinessException(EquipmentErrorCode.ARTIFACT_NOT_OWNED));
    }

    // 공통 (응답 변환)
    private List<ArtifactResponse> toSortedResponses(List<PlayerArtifact> artifacts) {
        return artifacts.stream()
                .sorted(Comparator.comparing(PlayerArtifact::getEquipSlot))
                .map(ArtifactResponse::from)
                .toList();
    }
}
