package com.swordmaster.shop.iap.google;

import java.time.Instant;

// 구글 영수증 검증 결과 중 서버가 쓰는 값만 담은 DTO
// (구글 라이브러리 타입을 서비스 계층으로 내보내지 않기 위해 따로 정의)
// api 엔 acknowledge 라는 항목도 존재한다고 한다 (전달 확인을 하였는가)
public record GooglePurchase(
        int     purchaseState,          // 결제 상태 (0 결제 완료, 1 취소, 2 보류)
        int     consumptionState,       // 소비처리가 되었는지 되지 않았다면 구매 불가   (0 미소비, 1 소비)
        String  orderId,                // 구글 주문 번호 (GPA.xxxx)
        String  obfuscatedAccountId,    // 클라이언트가 결제 시작 시 넣은 우리 계정 식별값
        Instant purchasedAt             // 결제 시각 (UTC)
) {
    public boolean isPurchased() { return purchaseState == 0; } //이미 결제가 완료되었는지 확인용도
    public boolean isPending()   { return purchaseState == 2; } //결제가 보류중인가
    public boolean isConsumed()  { return consumptionState == 1; } //해당 상품을 재구매할 수 있는 상태인가
}