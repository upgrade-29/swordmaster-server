package com.swordmaster.equipment.sword.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SwordService {
    private final PlayerRepository playerRepository;
    private final SwordTable       swordTable;

    private static final int SCALE = 10_000;

    // 조회
    public SwordResponse getMe(Long userId) {
        Player player = findPlayer(userId);

        return new SwordResponse(player.getSwordLevel());
    }

    // 강화 시도 (임시)
    @Transactional
    public SwordEnhanceResponse tryEnhance(Long userId) {
        Player player = findPlayer(userId);
        Sword  sword  = swordTable.findByLevel(player.getSwordLevel())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "검의 정보가 없습니다."));

        Double successRate = sword.successRate();
        Long   enhanceCost = sword.enhanceCost();

        if (successRate == null || enhanceCost == null) {
            throw new BusinessException("검을 강화할 수 없습니다.");
        }

        /////////////////////////////////////////////////////////////////////////
        // 재화 처리 로직 (임시)
        /////////////////////////////////////////////////////////////////////////

        int     rate      = (int) Math.round(successRate * SCALE);
        int     random    = ThreadLocalRandom.current().nextInt(SCALE);   // 0 <= random < SCALE
        boolean success   = random < rate;
        int     nextLevel = success ? sword.level() + 1 : swordTable.getFirst().level();

        player.changeSwordLevel(nextLevel);
        playerRepository.save(player);

        return new SwordEnhanceResponse(success, nextLevel);
    }

    // 플레이어 검색
    private Player findPlayer(Long userId) {
        return playerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "플레이어 정보가 없습니다."));
    }
}
