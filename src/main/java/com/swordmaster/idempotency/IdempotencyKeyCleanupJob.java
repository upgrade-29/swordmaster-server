package com.swordmaster.idempotency;

import com.swordmaster.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdempotencyKeyCleanupJob {
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    private static final Duration RETENTION = Duration.ofDays(1);

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanup() {
        idempotencyKeyRepository.deleteOlderThan(Instant.now().minus(RETENTION));
    }
}
