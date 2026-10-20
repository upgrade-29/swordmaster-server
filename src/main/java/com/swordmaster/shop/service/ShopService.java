package com.swordmaster.shop.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ShopService {
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;

    private final ShopCatalog shopCatalog;
    private final CurrencyService currencyService;
    private final PlayerService playerService;
    private final RewardService rewardService;
    private final IdempotencyService idempotencyService;
    private final ArtifactService artifactService;

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

        //3. 결제 진행 - 플레이어의 재화를 차감
        if (product.priceType().isCurrency()) {
            currencyService.consume(player, product.priceType().currency(),
                    product.price(), CurrencyReason.PURCHASE);
        }

        //4. 보상 지급
        List<Reward> granted = new ArrayList<>();

        for (Reward reward : rewards) {
            granted.addAll(rewardService.reward(player, reward, CurrencyReason.PURCHASE));
        }

        //5.구매를 위한 다른 작업들이 전부 처리되었다면 구매기록을 저장하고 응답을 반환
        CurrenciesResponse currencies = currencyService.getAll(player);

        List<ArtifactResponse> changedArtifacts = findChangedArtifacts(player, granted);

        ShopPurchaseResponse response = new ShopPurchaseResponse(granted, currencies, changedArtifacts);

        shopPurchaseLogRepository.save(new ShopPurchaseLog(userId, requestId, product, rewards, response));

        return response;
    }

    // 지급한 보상 중 아티팩트의 지급 후 상태. 클라이언트가 보유 목록에 바로 반영할 수 있게 응답에 담는다
    private List<ArtifactResponse> findChangedArtifacts(Player player, List<Reward> granted) {
        Set<String> codes = granted.stream()
                .filter(reward -> reward.rewardType() == RewardType.ARTIFACT)
                .map(Reward::rewardCode)
                .collect(Collectors.toSet());

        if (codes.isEmpty()) return List.of();

        return artifactService.getAll(player).stream()
                .filter(artifact -> codes.contains(artifact.code()))
                .toList();
    }

}
