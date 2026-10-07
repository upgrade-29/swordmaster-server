package com.swordmaster.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {

    private final String email;
    private final String password;
    private final String nickname;
}
