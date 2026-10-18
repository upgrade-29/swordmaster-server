package com.swordmaster.jwt.repository;

import com.swordmaster.jwt.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(Long userId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}
