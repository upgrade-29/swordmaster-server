package com.swordmaster.player.entity;

import com.swordmaster.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate     // 바뀐 컬럼만 UPDATE (검 강화가 전투에서 갱신한 clearedStage를 덮어쓰지 않도록)
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

    @ColumnDefault("0")     // 기존 행에 컬럼을 추가할 때의 값
    @Column(nullable = false)
    private int clearedStage;       // 지금까지 깬 가장 높은 스테이지 (없으면 0)

    public Player(User user, int swordLevel) {
        this.user       = user;
        this.swordLevel = swordLevel;
        clearedStage    = 0;
    }

    public void changeSwordLevel(int swordLevel) {
        this.swordLevel = swordLevel;
    }
}
