package com.swordmaster.equipment.artifact.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.EquipmentGrade;
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

    // 임시 설정 값
    private static final int          SLOT_COUNT    = 3;
    private static final CurrencyType CURRENCY_TYPE = CurrencyType.GOLD;

    // 생성
    public List<ArtifactResponse> createDefault(Player player) {
        List<PlayerArtifact> playerArtifacts = new ArrayList<>();
        List<String>         codeList        = constantTable.getTextList(ConstantKey.STARTER_ARTIFACTS);

        for (String code : codeList) {
            if (!artifactTable.existsByCode(code)) throw new BusinessException(HttpStatus.NOT_FOUND, "아티팩트 정보가 없습니다.");

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
        PlayerArtifact playerArtifact = find(player, code);
        Artifact       artifact       = find(code);

        int nextLevel = playerArtifact.getLevel() + 1;

        ArtifactEnhance artifactEnhance = find(artifact.grade(), nextLevel);

        int materialCount = playerArtifact.getMaterialCount();
        int requiredCount = artifactEnhance.materialCount();

        if (materialCount < requiredCount) throw new BusinessException("강화 재료가 부족합니다.");

        // 강화
        playerArtifact.changeLevel(nextLevel);
        playerArtifact.changeMaterialCount(materialCount - requiredCount);
        artifactRepository.save(playerArtifact);

        List<ArtifactResponse> artifactResponses = List.of(ArtifactResponse.from(playerArtifact));

        // 재화 소비
        long               cost               = artifactEnhance.gold();
        CurrenciesResponse currenciesResponse = currencyService.consume(player, CURRENCY_TYPE, cost, CurrencyReason.EQUIP_ENHANCE);

        return new ArtifactEnhanceResponse(
                artifactResponses,
                currenciesResponse
        );
    }

    // 장착 (및 변경)
    public List<ArtifactResponse> equip(Long userId, String code, int slot) {
        if (slot <= 0 || slot > SLOT_COUNT) throw new BusinessException("슬롯의 범위를 벗어났습니다.");

        Player               player   = playerService.getMe(userId);
        List<PlayerArtifact> equipped = doRemove(player, slot);     // 아티팩트 해제
        PlayerArtifact       required = find(player, code);

        // 새 아티팩트 장착
        required.changeEquipSlot(slot);
        equipped.add(required);
        artifactRepository.save(required);

        return equipped.stream()
                .sorted(Comparator.comparing(PlayerArtifact::getEquipSlot))
                .map(ArtifactResponse::from)
                .toList();
    }

    // 해제
    public List<ArtifactResponse> remove(Long userId, int slot) {
        if (slot <= 0 || slot > SLOT_COUNT) throw new BusinessException("슬롯의 범위를 벗어났습니다.");

        Player               player   = playerService.getMe(userId);
        List<PlayerArtifact> equipped = doRemove(player, slot);

        return equipped.stream()
                .sorted(Comparator.comparing(PlayerArtifact::getEquipSlot))
                .map(ArtifactResponse::from)
                .toList();
    }

    @Transactional
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

    private List<PlayerArtifact> doRemove(Player player, int slot) {
        List<PlayerArtifact> equipped = artifactRepository.findAllByPlayerAndEquipSlotIsNotNull(player);

        equipped.removeIf(
                artifact -> {
                    if (artifact.getEquipSlot() != slot) return false;

                    artifact.changeEquipSlot(null);
                    artifactRepository.save(artifact);

                    return true;
                }
        );

        return equipped;
    }

    // 공통 (조회)
    private PlayerArtifact find(Player player, String code) {
        return artifactRepository.findByPlayerAndCode(player, code)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "아티팩트가 없습니다."));
    }

    private Artifact find(String code) {
        return artifactTable.findByCode(code)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "아티팩트 정보가 없습니다."));
    }

    private ArtifactEnhance find(EquipmentGrade grade, int level) {
        return artifactEnhanceTable.findByGradeAndLevel(grade, level)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "아티팩트 강화 정보가 없습니다."));
    }
}
