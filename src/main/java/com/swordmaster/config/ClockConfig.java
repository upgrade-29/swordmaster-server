package com.swordmaster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {
    // 현재 시각 (테스트에서 시간을 직접 흘릴 수 있도록 주입해서 사용)
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
