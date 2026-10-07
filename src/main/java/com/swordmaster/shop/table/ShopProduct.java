package com.swordmaster.shop.table;

import com.swordmaster.shop.CurrencyType;

public record ShopProduct(
        String       productCode,   // 상품 코드 (구매 요청의 productCode)
        String       name,          // 표시 이름
        String       category,      // 카테고리
        String       iconCode,      // 아이콘 리소스 이름
        CurrencyType priceType,     // 결제 재화 (GOLD, DIAMOND)
        long         price          // 가격
) {}