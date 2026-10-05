package com.swordmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SwordmasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwordmasterApplication.class, args);
    }

}
