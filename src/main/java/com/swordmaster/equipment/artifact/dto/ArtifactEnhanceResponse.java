package com.swordmaster.equipment.artifact.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;

import java.util.List;

public record ArtifactEnhanceResponse(
        List<ArtifactResponse> artifactResponses,
        CurrenciesResponse     currenciesResponse
) {
}
