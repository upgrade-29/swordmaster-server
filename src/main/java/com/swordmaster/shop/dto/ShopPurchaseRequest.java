package com.swordmaster.shop.dto;

import lombok.Getter;

@Getter
public class ShopPurchaseRequest {
    private final String requestId;
    private final String productCode;

    public ShopPurchaseRequest(String requestId, String productCode) {
        this.requestId = requestId;
        this.productCode = productCode;
    }
}
