package com.swordmaster.shop;

import com.swordmaster.currency.CurrencyType;


//무료, 현금 결제 추가에 따라 enum 분리
public enum PriceType {
    GOLD(CurrencyType.GOLD),
    DIAMOND(CurrencyType.DIAMOND),
    FREE(null),
    CASH(null);

    private final CurrencyType currency;    // 게임 재화로 결제 시 사용하는 필드

    PriceType(CurrencyType currency) {
        this.currency = currency;
    }

    public CurrencyType currency() {
        return currency;
    }

    public boolean isCurrency() {
        return currency != null;
    }

}
