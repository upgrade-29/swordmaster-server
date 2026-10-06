package com.swordmaster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {//비밀번호를 해싱해서 그대로 유출되는 것을 방지하기 위한 spring의 기능

    @Bean // service에서 해당 클래스를 넣지 않는것은 인터페이스를 구현한 것이라고 생각하면 편하다(나도 내가 뭘 적는지 모르겠다)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}