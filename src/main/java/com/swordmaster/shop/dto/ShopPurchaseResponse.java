package com.swordmaster.shop.dto;

import com.swordmaster.shop.Currency;
import com.swordmaster.shop.Reward;
import lombok.Getter;

import java.util.List;

@Getter
public class ShopPurchaseResponse {
    private List<Reward> rewards; //보상

    private Currency currencies; //현재 남은 재화?

    public ShopPurchaseResponse(List<Reward> rewards, Currency currencies) {
        this.rewards = rewards;
        this.currencies = currencies;
    }
}
