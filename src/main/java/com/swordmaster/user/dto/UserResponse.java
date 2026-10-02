package com.swordmaster.user.dto;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Long gold,
        int diamond,
        int swordLevel
        //,List<ArtifactResponse> artifacts
) {
}
