package com.swordmaster.equipment.sword.dto;

import com.swordmaster.player.entity.Player;

public record SwordResponse(int level) {
    public static SwordResponse from(Player player) {
        return new SwordResponse(player.getSwordLevel());
    }
}
