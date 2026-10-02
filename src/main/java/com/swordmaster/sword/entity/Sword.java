package com.swordmaster.sword.entity;

import com.swordmaster.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "swords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sword {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int level = 0;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt;

    public Sword(User user){
        this.user = user;
    }
}
