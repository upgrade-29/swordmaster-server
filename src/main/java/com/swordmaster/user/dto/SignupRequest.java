package com.swordmaster.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {

    @Email
    @NotBlank
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "이메일 형식이 아닙니다.")
    private final String email;
    @NotBlank
    @Pattern(regexp = "^\\S+$", message = "비밀번호에 공백을 포함할 수 없습니다.")
    private final String password;
}
