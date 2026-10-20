package com.swordmaster.user.repository;

import com.swordmaster.common.enums.LoginProvider;
import com.swordmaster.user.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderUserId(
            LoginProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            LoginProvider provider,
            String providerUserId
    );
}
