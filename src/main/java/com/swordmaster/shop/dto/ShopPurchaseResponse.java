package com.swordmaster.shop.dto;

import java.util.List;


public record ShopPurchaseResponse(
        List<Reward> rewards,
        Currencies currencies
) {}
