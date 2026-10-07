package com.swordmaster.currency;

import com.swordmaster.player.repository.PlayerCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ApplicationRunner 클래스에 @Component 어노테이션을 설정하면
// 스프링이 서버 준비를 마친 후 자동으로 run()을 한 번 실행
@Component
@RequiredArgsConstructor
public class CurrencyBackfillRunner implements ApplicationRunner {
    private final PlayerCurrencyRepository playerCurrencyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 서버 실행 시 새로운 type이 존재하면 커스텀 SQL을 실행하여 INSERT 수행
        for (CurrencyType type : CurrencyType.values())
            playerCurrencyRepository.insertMissingRows(type.name());
    }
}
