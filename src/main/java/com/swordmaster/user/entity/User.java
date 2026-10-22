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

    //게스트 로그인을 위해서 nullable 제거, 하지만 dto에서 notblank를 통해서 빈값을 받기를 방지한다.
    //기존 user에서 하던 작업들이 useridentity로 이동하여서 email의 unique와 nullable조건을 제거.
    @Column(length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

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

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = Instant.now();
    }
}
