package com.swordmaster.idempotency.repository;

import com.swordmaster.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByUserIdAndRequestId(Long userId, String requestId);

    // 기준 시각보다 오래된 키 제거 (삭제된 행 수 반환)
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.createdAt < :before")
    void deleteOlderThan(@Param("before")Instant before);
}
