package com.swordmaster.shop.iap;

import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.service.RewardService;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.entity.ShopPurchaseLog;
import com.swordmaster.shop.iap.google.GooglePurchase;
import com.swordmaster.shop.repository.ShopPurchaseLogRepository;
import com.swordmaster.shop.table.ShopCatalog;
import com.swordmaster.shop.table.ShopProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 검증이 끝난 영수증에 대해 "영수증 저장 → 지급 → 구매 기록"을 하나의 트랜잭션으로 처리
// (IapService와 분리한 이유: 같은 클래스 안에서 호출하면 @Transactional이 적용되지 않음)
@Service
@RequiredArgsConstructor
public class IapGrantService {
    private final PlayerService             playerService;
    private final ShopCatalog               shopCatalog;
    private final RewardService             rewardService;
    private final CurrencyService           currencyService;
    private final IapReceiptRepository      receiptRepository;
    private final ShopPurchaseLogRepository purchaseLogRepository;

    @Transactional
    public ShopPurchaseResponse grant(Long userId, ShopProduct product, String purchaseToken, GooglePurchase purchase) {
        Player player = playerService.getMe(userId);

        // 1. 영수증 먼저 저장: 같은 토큰이 동시에 들어오면 여기서 UNIQUE 위반 → 지급 전에 실패
        receiptRepository.saveAndFlush(new IapReceipt(
                userId, product.productCode(), purchaseToken, purchase.orderId(), purchase.purchasedAt()));

        // 2. 보상 지급 (가챠 상자 등도 기존 RewardService가 그대로 처리)
        List<Reward> rewards = shopCatalog.findRewards(product.productCode());
        List<Reward> granted = new ArrayList<>();

        for (Reward reward : rewards)
            granted.addAll(rewardService.reward(player, reward, CurrencyReason.PURCHASE));

        ShopPurchaseResponse response = new ShopPurchaseResponse(granted, currencyService.getAll(player));

        // 3. 기존 구매 기록에도 남김 (문의 대응을 한곳에서). requestId는 토큰에서 항상 같은 값으로 생성
        UUID requestId = UUID.nameUUIDFromBytes(purchaseToken.getBytes(StandardCharsets.UTF_8));
        purchaseLogRepository.save(new ShopPurchaseLog(userId, requestId, product, rewards, response));

        return response;
    }
}