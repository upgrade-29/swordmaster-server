package com.swordmaster.user.entity;

import com.swordmaster.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //TODO : email == login id 이후에 변경가능
    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING) //db에서 관리자로 변경이 가능, 그래야만 시스템 공지를 사용할 수 있음
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    private Instant lastLoginAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant  updatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant  createdAt;

    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = Instant.now();
    }
}
