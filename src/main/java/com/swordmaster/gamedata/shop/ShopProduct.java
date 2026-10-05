package com.swordmaster.gamedata.shop;

import com.swordmaster.shop.CurrencyType;

//구글 스프레드 시트의 ShopProduct 데이터의 열
public record ShopProduct (
    String productCode,
    String name,
    String category,
    String iconCode,
    CurrencyType priceType,
    long price
){}
