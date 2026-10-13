package com.swordmaster.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlayerCreateRequest(
        @NotBlank(message = "닉네임을 입력해 주세요. (공백 불가)")
        @Size(max = 10, message = "닉네임은 10자 이하로 입력해 주세요.")
        @Pattern(regexp = "\\S+", message = "닉네임에는 공백을 사용할 수 없습니다.")
        String nickname
) {
}
