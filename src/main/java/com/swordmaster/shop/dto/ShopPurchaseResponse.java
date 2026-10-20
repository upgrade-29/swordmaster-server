package com.swordmaster.shop.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.reward.Reward;

import java.util.List;


public record ShopPurchaseResponse(
        List<Reward> rewards,
        CurrenciesResponse currencies,
        List<ArtifactResponse> changedArtifacts   // 이번 구매로 새로 생기거나 재료가 늘어난 아티팩트 (없으면 빈 리스트)
) {}
