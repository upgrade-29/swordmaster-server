package com.swordmaster.player.entity;

import com.swordmaster.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)             // 필요시에만 조회, Not Null (JPA에서 인식)
    @JoinColumn(name = "user_id", nullable = false, unique = true)  // Not Null, 유저당 하나만
    private User user;

    @Column(nullable = false)
    private int swordLevel;

    public Player(User user, int swordLevel) {
        this.user       = user;
        this.swordLevel = swordLevel;
    }
}
