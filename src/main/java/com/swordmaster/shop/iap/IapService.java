package com.swordmaster.shop.iap;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.shop.PriceType;
import com.swordmaster.shop.ShopErrorCode;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.iap.google.GooglePlayVerifier;
import com.swordmaster.shop.iap.google.GooglePlayVerifier.GooglePlayUnavailableException;
import com.swordmaster.shop.iap.google.GooglePlayVerifier.InvalidPurchaseException;
import com.swordmaster.shop.iap.google.GooglePurchase;
import com.swordmaster.shop.table.ShopCatalog;
import com.swordmaster.shop.table.ShopProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

// 구글 결제 흐름: 중복 확인 → 구글 검증 → 지급(트랜잭션) → consume
// 외부 API 호출을 DB 트랜잭션 밖에 두기 위해 이 클래스에는 @Transactional을 붙이지 않음
@Slf4j
@Service
@RequiredArgsConstructor
public class IapService {
    private final ShopCatalog                        shopCatalog;
    private final IapGrantService                    grantService;
    private final IapReceiptRepository               receiptRepository;
    private final CurrencyService                    currencyService;
    private final PlayerService                      playerService;
    private final ObjectProvider<GooglePlayVerifier> verifierProvider;   // 설정에 따라 없을 수 있음
    private final Clock                              clock;

    public ShopPurchaseResponse purchaseWithGoogle(Long userId, String productCode, String purchaseToken) {
        GooglePlayVerifier verifier = verifierProvider.getIfAvailable();
        if (verifier == null) throw new BusinessException(IapErrorCode.IAP_DISABLED);

        // 0. 현금 상품인지 확인
        ShopProduct product = shopCatalog.findProduct(productCode)
                .orElseThrow(() -> new BusinessException(ShopErrorCode.PRODUCT_NOT_FOUND));
        if (product.priceType() != PriceType.CASH)
            throw new BusinessException(IapErrorCode.NOT_CASH_PRODUCT);

        // 1. 이미 처리한 영수증이면 다시 지급하지 않음 (재전송 대응)
        var existing = receiptRepository.findByPurchaseToken(purchaseToken);
        if (existing.isPresent()) return alreadyProcessed(userId, existing.get());

        // 2. 구글 검증
        GooglePurchase purchase;
        try {
            purchase = verifier.verify(productCode, purchaseToken);
        } catch (InvalidPurchaseException e) {
            throw new BusinessException(IapErrorCode.INVALID_RECEIPT);
        } catch (GooglePlayUnavailableException e) {
            throw new BusinessException(IapErrorCode.STORE_UNAVAILABLE);
        }

        if (purchase.isPending())
            throw new BusinessException(IapErrorCode.PURCHASE_PENDING);
        if (!purchase.isPurchased() || purchase.isConsumed())
            throw new BusinessException(IapErrorCode.INVALID_RECEIPT);
        if (!String.valueOf(userId).equals(purchase.obfuscatedAccountId()))
            throw new BusinessException(IapErrorCode.RECEIPT_OWNER_MISMATCH);

        // 3. 지급 (트랜잭션)
        ShopPurchaseResponse response;
        try {
            response = grantService.grant(userId, product, purchaseToken, purchase);
        } catch (DataIntegrityViolationException e) {
            // 같은 토큰이 동시에 들어와 다른 요청이 먼저 지급함
            return alreadyProcessed(userId, receiptRepository.findByPurchaseToken(purchaseToken).orElseThrow());
        }

        // 4. 구글 영수증 소비 처리 (실패해도 지급은 끝났으니 성공 응답, 스케줄러가 재시도)
        consumeQuietly(verifier, productCode, purchaseToken);

        return response;
    }

    // 이미 지급된 영수증: 보상은 비우고 현재 재화만 반환
    private ShopPurchaseResponse alreadyProcessed(Long userId, IapReceipt receipt) {
        if (!receipt.getUserId().equals(userId))
            throw new BusinessException(IapErrorCode.RECEIPT_OWNER_MISMATCH);

        return new ShopPurchaseResponse(List.of(), currencyService.getAll(playerService.getMe(userId)));
    }

    // 구글 영수증 소비 처리 + 영수증 상태 CONSUMED로 변경 (스케줄러에서도 재사용)
    public void consumeQuietly(GooglePlayVerifier verifier, String productCode, String purchaseToken) {
        try {
            verifier.consume(productCode, purchaseToken);

            receiptRepository.findByPurchaseToken(purchaseToken).ifPresent(r -> {
                r.markConsumed(clock.instant());
                receiptRepository.save(r);
            });
        } catch (RuntimeException e) {
            log.warn("[IAP] 영수증 소비 처리 실패, 스케줄러가 재시도: product={}", productCode);   // 토큰 원문은 로그 금지
        }
    }
}