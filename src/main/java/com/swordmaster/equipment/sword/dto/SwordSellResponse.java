package com.swordmaster.equipment.sword.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.reward.Reward;

import java.util.List;

public record SwordSellResponse(
        SwordResponse      swordResponse,
        List<Reward>       rewardResponses,
        CurrenciesResponse currenciesResponse
) {
}
