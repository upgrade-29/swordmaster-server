package com.swordmaster.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "구글 ID 토큰이 필요합니다.")
        @Size(max = 16384, message = "구글 ID 토큰이 너무 깁니다.")
        String idToken
) {
}