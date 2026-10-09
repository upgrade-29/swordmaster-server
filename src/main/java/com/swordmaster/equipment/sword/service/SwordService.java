package com.swordmaster.equipment.sword.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.sword.dto.SwordSellResponse;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SwordService {
    private final SwordTable       swordTable;
    private final PlayerService    playerService;
    private final PlayerRepository playerRepository;
    private final CurrencyService  currencyService;

    private static final int          SCALE         = 10_000;
    private static final CurrencyType CURRENCY_TYPE = CurrencyType.GOLD;

    // 강화
    @Transactional
    public SwordEnhanceResponse enhance(Long userId) {
        Player player      = playerService.find(userId);
        Sword  sword       = find(player);
        Double successRate = sword.successRate();
        Long   enhanceCost = sword.enhanceCost();

        if (successRate == null || enhanceCost == null) throw new BusinessException("검을 강화할 수 없습니다.");

        int     rate      = (int) Math.round(successRate * SCALE);
        int     random    = ThreadLocalRandom.current().nextInt(SCALE);   // 0 <= random < SCALE
        boolean success   = random < rate;
        int     nextLevel = success ? sword.level() + 1 : swordTable.getFirst().level();

        // 재화 소비
        long currency = currencyService.consume(player, CURRENCY_TYPE, enhanceCost, CurrencyReason.EQUIP_ENHANCE);

        player.changeSwordLevel(nextLevel);
        playerRepository.save(player);

        return new SwordEnhanceResponse(success, nextLevel, currency);
    }

    // 판매
    public SwordSellResponse sell(Long userId) {
        Player player = playerService.find(userId);
        Sword  sword  = find(player);
        long   reward = sword.sellPrice();
        int    level  = swordTable.getFirst().level();

        // 재화 획득
        long currency = currencyService.grant(player, CURRENCY_TYPE, reward, CurrencyReason.EQUIP_SELL);

        player.changeSwordLevel(level);
        playerRepository.save(player);

        return new SwordSellResponse(level, reward, currency);
    }

    // 공통 (Sword 검색)
    private Sword find(Player player) {
        return swordTable.findByLevel(player.getSwordLevel())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "검의 정보가 없습니다."));
    }
}
