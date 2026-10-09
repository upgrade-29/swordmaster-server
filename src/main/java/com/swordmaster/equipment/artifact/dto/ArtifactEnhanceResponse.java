package com.swordmaster.equipment.artifact.dto;

public record ArtifactEnhanceResponse(
        ArtifactResponse artifact,
        long             gold
) {
}
