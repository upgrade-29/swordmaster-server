package com.swordmaster.currency;

import com.swordmaster.currency.repository.CurrencyHistoryRepository;
import com.swordmaster.player.entity.PlayerCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 매일 정해진 시간에 PlayerCurrency 테이블의 모든 행에 대하여 CurrencyHistory 테이블과 검증(비교)
// 일치하지 않는 조회 결과를 로그로 반환
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyReconciliationJob {
    private final CurrencyHistoryRepository currencyHistoryRepository;

    @Scheduled(cron = "0 0 1 * * *")    // 매일 새벽 1시
    @Transactional(readOnly = true)
    public void check() {
        List<PlayerCurrency> mismatched = currencyHistoryRepository.findMismatched();

        if (mismatched.isEmpty()) return;

        log.error("재화 내역이 일치하지 않는 데이터가 존재합니다.");

        for (PlayerCurrency currency : mismatched) {
            log.error("playerId={}, type={}, amount={}",
                    currency.getPlayer().getId(),
                    currency.getType(),
                    currency.getAmount()
            );
        }
    }
}
