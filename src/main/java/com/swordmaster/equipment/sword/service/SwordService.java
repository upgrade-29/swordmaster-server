package com.swordmaster.equipment.sword.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.EnhanceResult;
import com.swordmaster.equipment.EquipmentErrorCode;
import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.equipment.sword.dto.SwordRequest;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.equipment.sword.dto.SwordSellResponse;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.message.service.MessageService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.service.RankingService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class SwordService {
    private final SwordTable         swordTable;
    private final PlayerService      playerService;
    private final CurrencyService    currencyService;
    private final IdempotencyService idempotencyService;
    private final RankingService     rankingService;
    private final MessageService     messageService;

    // 임시 설정 값
    private static final int          SCALE         = 10_000;
    private static final CurrencyType CURRENCY_TYPE = CurrencyType.GOLD;
    private static final int          NOTICE_LEVEL  = 7;

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
        // 레벨 검사
        if (player.getSwordLevel() != expectedLevel) throw new BusinessException(EquipmentErrorCode.LEVEL_MISMATCH);

        // 최대 레벨 검사
        if (player.getSwordLevel() == swordTable.getLast().level()) throw new BusinessException(EquipmentErrorCode.SWORD_MAX_LEVEL);

        Sword  sword       = findByTable(player);
        Double successRate = sword.successRate();
        Long   cost        = sword.enhanceCost();

        if (successRate == null || cost == null) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "검을 강화할 수 없습니다.");

        int           rate    = (int)Math.round(successRate * SCALE);
        int           random  = ThreadLocalRandom.current().nextInt(SCALE);    // 0 <= random < SCALE
        boolean       success = random < rate;
        EnhanceResult result  = success ? EnhanceResult.SUCCESS : EnhanceResult.DESTROYED;
        int           level   = success ? sword.level() + 1 : swordTable.getFirst().level();

        // 재화 소비 (+ 재화량 검사)
        CurrenciesResponse currenciesResponse = currencyService.consume(player, CURRENCY_TYPE, cost, CurrencyReason.EQUIP_ENHANCE);

        // 검 레벨 변경(강화)
        SwordResponse swordResponse = playerService.changeSwordLevel(player, level);

        // 랭킹 등록 시도 (값은 DB(Ranking 테이블)의 기록 SQL 내부에서 비교한 후에 기록 시도)
        rankingService.recordRanking(player, RankingType.SWORD_LEVEL, level);
        rankingService.recordCombatPower(player);   // 전투력 랭킹 기록

        Long userId = player.getUser().getId();
        String displayName = player.getNickname();
        String content = success
                ? "검 " + level + "강에 성공했습니다!"
                : "검 " + sword.level() + "강 → "
                        + (sword.level() + 1) + "강 강화에 실패하여 검이 파괴되었습니다.";

        // DB 저장이 확정된 뒤 강화 결과를 알린다.(트렌젝션 내부에서 강화 결과 알림보다 앞에 있지만, 강화결과와 db 적용 이후 적용이된다.)
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messageService.sendPrivateSystemNotice(userId, content);

                        if (success && level >= NOTICE_LEVEL) {
                            messageService.sendEnhancementNotice(
                                    userId, displayName, level
                            );
                        }
                    }
                }
        );

        return new SwordEnhanceResponse(
                result,
                swordResponse,
                currenciesResponse
        );
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
        // 레벨 검사
        if (player.getSwordLevel() != expectedLevel) throw new BusinessException(EquipmentErrorCode.LEVEL_MISMATCH);

        Sword sword = findByTable(player);
        int   level = swordTable.getFirst().level();

        // 판매 레벨 검사
        if (player.getSwordLevel() == level) throw new BusinessException(EquipmentErrorCode.SWORD_NOT_SELLABLE);

        // 검 레벨 변경(초기화)
        SwordResponse swordResponse = playerService.changeSwordLevel(player, level);

        long price = sword.sellPrice();

        // 보상 내역
        List<Reward> rewardResponses = List.of(new Reward(RewardType.GOLD, null, price));

        // 재화 획득
        CurrenciesResponse currenciesResponse = currencyService.grant(player, CURRENCY_TYPE, price, CurrencyReason.EQUIP_SELL);

        // 전투력 랭킹 기록
        rankingService.recordCombatPower(player);

        return new SwordSellResponse(
                swordResponse,
                rewardResponses,
                currenciesResponse
        );
    }

    // 공통 (조회)
    private Sword findByTable(Player player) {
        return swordTable.findByLevel(player.getSwordLevel())
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "검을 강화할 수 없습니다."));
    }
}
