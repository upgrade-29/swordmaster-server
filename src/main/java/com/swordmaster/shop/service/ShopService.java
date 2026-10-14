package com.swordmaster.shop.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.service.RewardService;
import com.swordmaster.shop.ShopErrorCode;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.entity.ShopPurchaseLog;
import com.swordmaster.shop.repository.ShopPurchaseLogRepository;
import com.swordmaster.shop.table.ShopCatalog;
import com.swordmaster.shop.table.ShopProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ShopService {
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;

    private final ShopCatalog shopCatalog;
    private final CurrencyService currencyService;
    private final PlayerService playerService;
    private final RewardService rewardService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request) {
        UUID requestId = request.requestId();

        //공용 requestId 기록 모듈이 재결제를 방지하게 됨. (보관 기간은 1일)
        return idempotencyService.execute(
                userId,
                requestId.toString(),
                ShopPurchaseResponse.class,
                () -> doPurchase(userId, requestId, request.productCode())
        );
    }

    private ShopPurchaseResponse doPurchase(Long userId, UUID requestId, String productCode) {
        //1. 플레이어 조회 - userId에 대응하는 player가 존재하는 지 확인하며 재화를 변동할 때 대입할 player를 가져온다.
        Player player = playerService.getMe(userId);

        //2. 상품 조회 - 시트의 데이터에서 productCode 에 대응하는 상품과 그 상품목록을 가져옴
        ShopProduct product = shopCatalog.findProduct(productCode)
                .orElseThrow(() -> new BusinessException(ShopErrorCode.PRODUCT_NOT_FOUND));

        List<Reward> rewards = shopCatalog.findRewards(productCode);

        //3. 결제 진행 - 플레이어의 재화를 차감 및 보상을 지급
        CurrencyType priceType = product.priceType();

        //유저의 재화에서 가격만큼 차감을 시도.
        currencyService.consume(player, priceType, product.price(), CurrencyReason.PURCHASE);

        //재화가 차감되었다면 유저에게 보상을 지급하고 지급이 완료된 보상들을 리스트로 저장
        List<Reward> granted = new ArrayList<>();

        for (Reward reward : rewards) {
            granted.addAll(rewardService.reward(player, reward, CurrencyReason.PURCHASE));
        }

        //차감 및 보상 완료
        CurrenciesResponse currencies = currencyService.getAll(player);

        //4.구매를 위한 다른 작업들이 전부 처리되었다면 구매기록을 저장하고 응답을 반환
        ShopPurchaseResponse response = new ShopPurchaseResponse(granted, currencies);

        shopPurchaseLogRepository.save(new ShopPurchaseLog(userId, requestId, product, rewards, response));

        return response;
    }

}
