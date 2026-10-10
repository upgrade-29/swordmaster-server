package com.swordmaster.common.table.constant;

public enum ConstantKey {
    CRIT_MULTIPLIER          (ConstantType.DECIMAL),
    BASE_BATTLE_TIME         (ConstantType.LONG),
    NORMAL_BATTLE_TIME_LIMIT (ConstantType.LONG),
    BOSS_BATTLE_TIME_LIMIT   (ConstantType.LONG),
    HEAL_RATE                (ConstantType.DECIMAL),
    INITIAL_GOLD             (ConstantType.LONG),
    STARTER_ARTIFACTS        (ConstantType.TEXT_LIST),
    NOTICE_SWORD_LEVEL       (ConstantType.INT),
    NOTICE_DESTROY_LEVEL     (ConstantType.INT);

    private final ConstantType type;

    ConstantKey(ConstantType type) { this.type = type; }

    public ConstantType type() { return type; }
}
