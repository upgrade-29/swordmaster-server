package com.swordmaster.equipment.sword.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.equipment.EnhanceResult;

public record SwordEnhanceResponse(
        EnhanceResult      result,
        SwordResponse      swordResponse,
        CurrenciesResponse currenciesResponse
) {
}
