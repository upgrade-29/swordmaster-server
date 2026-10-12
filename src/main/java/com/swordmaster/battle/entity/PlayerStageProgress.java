package com.swordmaster.battle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 플레이어의 스테이지 진행도 (플레이어당 한 행, 처음 클리어할 때 생성)
// Player와 따로 두어 검 강화(swordLevel) 같은 다른 변경과 서로 덮어쓰지 않음
@Entity
@Table(name = "player_stage_progresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerStageProgress {
    @Id
    @Column(name = "player_id")
    private Long playerId;              // 굳이 조인을 하지 않고 ID 값만 참조

    @Column(name = "cleared_stage", nullable = false)
    private int clearedStage;           // 지금까지 깬 가장 높은 스테이지

    public PlayerStageProgress(Long playerId, int clearedStage) {
        this.playerId     = playerId;
        this.clearedStage = clearedStage;
    }

    // 도전 가능한 스테이지는 1 ~ clearedStage + 1 (행이 없으면 clearedStage 0)
    public static boolean isUnlocked(int clearedStage, int stage) {
        return stage >= 1 && stage <= clearedStage + 1;
    }

    // 최고 기록일 때만 갱신
    public void clear(int stage) {
        clearedStage = Math.max(clearedStage, stage);
    }
}
