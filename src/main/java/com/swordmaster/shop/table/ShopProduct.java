package com.swordmaster.shop.table;

import com.swordmaster.shop.PriceType;

public record ShopProduct(
        String productCode,   // 상품 코드 (구매 요청의 productCode)
        String name,          // 표시 이름
        String category,      // 카테고리
        String iconCode,      // 아이콘 리소스 이름
        PriceType priceType,     // 결제 방식 GOLD,DIAMOND,FREE/CASH는 아직 미구현 상태라 검증에서 차단
        long price          // 가격 (FREE는 0)
) {
}