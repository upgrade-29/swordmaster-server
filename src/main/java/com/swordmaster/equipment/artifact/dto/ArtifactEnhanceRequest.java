package com.swordmaster.equipment.artifact.dto;

import jakarta.validation.constraints.NotNull;

public record ArtifactEnhanceRequest(
        @NotNull(message = "요청 ID가 필요합니다.") String requestId
) {
}
