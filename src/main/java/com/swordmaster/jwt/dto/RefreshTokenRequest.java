package com.swordmaster.jwt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank
        @Size(min = 80, max = 80)
        String refreshToken
) {}
