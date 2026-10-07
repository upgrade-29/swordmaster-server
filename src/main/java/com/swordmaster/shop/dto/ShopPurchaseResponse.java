package com.swordmaster.shop.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;

import java.util.List;


public record ShopPurchaseResponse(
        List<Reward> rewards,
        CurrenciesResponse currenciesResponse
) {}
