package com.swordmaster.user.entity;

import com.swordmaster.common.enums.LoginProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_identities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_identity_provider_subject",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginProvider provider;

    // (다른 로그인 api가 제공하는 특정 값)소셜 로그인이면 제공자가 발급한 사용자 고유 ID
    // LOCAL이면 로그인 이메일로 반환
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    // LOCAL 로그인에서만 사용하며 암호화한 해시를 저장(로컬만을 위한 위의 값과 동일한 기능)
    private String passwordHash;

    // 제공자로부터 받은 이메일 정보. 계정 연결 기준으로 사용하지 않는다.
    @Column(length = 254)
    private String email;

    //여러가지 로그인 방법에 따른 생성자
    public static UserIdentity createLocal(
            User user,
            String loginEmail,
            String passwordHash
    ) {
        UserIdentity identity = new UserIdentity();
        identity.user = user;
        identity.provider = LoginProvider.LOCAL;
        identity.providerUserId = loginEmail;
        identity.passwordHash = passwordHash;
        identity.email = loginEmail;
        return identity;
    }

    public static UserIdentity createGuest(
            User user,
            String email
    ) {
        UserIdentity identity = new UserIdentity();
        identity.user = user;
        identity.provider = LoginProvider.GUEST;
        identity.providerUserId = UUID.randomUUID().toString();//게스트용 임시값
        identity.passwordHash = null;
        identity.email = email;
        return identity;
    }

    public static UserIdentity createGoogle(
            User user,
            String googleSub,
            String email
    ) {
        UserIdentity identity = new UserIdentity();
        identity.user = user;
        identity.provider = LoginProvider.GOOGLE;
        identity.providerUserId = googleSub;
        identity.passwordHash = null;
        identity.email = email;
        return identity;
    }
}