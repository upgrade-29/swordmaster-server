package com.swordmaster.gacha.table;

import com.swordmaster.equipment.EquipmentGrade;

// 가챠 시트 grade 열 전용. NONE = 등급 랜덤 아님 (시트·클라이언트의 None 관례와 맞춤)
public enum GachaGrade {
    NONE,
    COMMON,
    RARE,
    EPIC,
    LEGENDARY;


    // 가챠 시트의 등급을 아티팩트 시트의 등급으로 변환 (NONE → null)
    public EquipmentGrade toEquipmentGrade() {
        return this == NONE ? null : EquipmentGrade.valueOf(name());
    }
}