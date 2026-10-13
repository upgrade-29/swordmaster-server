package com.swordmaster.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {

    @Email
    @NotBlank
    private final String email;
    @NotBlank
    private final String password;
}
