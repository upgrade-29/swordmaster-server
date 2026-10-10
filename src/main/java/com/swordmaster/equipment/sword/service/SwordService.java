package com.swordmaster.equipment.sword.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.sword.dto.SwordRequest;
import com.swordmaster.equipment.sword.dto.SwordSellResponse;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.idempotency.service.IdempotencyService;
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
@Transactional
public class SwordService {
    private final SwordTable         swordTable;
    private final PlayerService      playerService;
    private final PlayerRepository   playerRepository;
    private final CurrencyService    currencyService;
    private final IdempotencyService idempotencyService;

    // 임시 설정 값 (클라이언트와 일치시키기 위해 외부에서 불러올 필요가 있음)
    private static final int          SCALE         = 10_000;
    private static final CurrencyType CURRENCY_TYPE = CurrencyType.GOLD;

    // 강화
    public SwordEnhanceResponse enhance(Long userId, SwordRequest request) {
        Player player = playerService.getMe(userId);

        return idempotencyService.execute(
                userId,
                request.requestId(),
                SwordEnhanceResponse.class,
                () -> doEnhance(player, request.expectedLevel())
        );
    }

    private SwordEnhanceResponse doEnhance(Player player, int expectedLevel) {
        if (player.getSwordLevel() != expectedLevel) throw new BusinessException("검의 DB 정보와 일치하지 않습니다.");

        Sword  sword       = find(player);
        Double successRate = sword.successRate();
        Long   cost        = sword.enhanceCost();

        if (successRate == null || cost == null) throw new BusinessException("검을 강화할 수 없습니다.");

        int rate = (int)Math.round(successRate * SCALE);
        int random = ThreadLocalRandom.current().nextInt(SCALE);    // 0 <= random < SCALE
        int level = (random < rate) ? sword.level() + 1 : swordTable.getFirst().level();

        // 재화 소비
        long currency = currencyService.consume(player, CURRENCY_TYPE, cost, CurrencyReason.EQUIP_ENHANCE);

        player.changeSwordLevel(level);
        playerRepository.save(player);

        return new SwordEnhanceResponse(expectedLevel == level, level, currency);
    }

    // 판매
    public SwordSellResponse sell(Long userId, SwordRequest request) {
        Player player = playerService.getMe(userId);

        return idempotencyService.execute(
                userId,
                request.requestId(),
                SwordSellResponse.class,
                () -> doSell(player, request.expectedLevel())
        );
    }

    private SwordSellResponse doSell(Player player, int expectedLevel) {
        if (player.getSwordLevel() != expectedLevel) throw new BusinessException("검의 DB 정보와 일치하지 않습니다.");

        Sword sword = find(player);
        int   level = swordTable.getFirst().level();

        if (player.getSwordLevel() == level) throw new BusinessException("검을 판매할 수 없습니다");

        // 재화 획득
        long reward   = sword.sellPrice();
        long currency = currencyService.grant(player, CURRENCY_TYPE, reward, CurrencyReason.EQUIP_SELL);

        player.changeSwordLevel(level);
        playerRepository.save(player);

        return new SwordSellResponse(level, reward, currency);
    }

    // 공통 (검색)
    private Sword find(Player player) {
        return swordTable.findByLevel(player.getSwordLevel())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "검의 정보가 없습니다."));
    }
}
