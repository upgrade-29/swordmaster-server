package com.swordmaster.gacha.table;

import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;

public record GachaEntry(
        String     gachaCode,       // 가챠(상자) 코드 — ShopProductReward의 GACHA_BOX rewardCode와 연결
        RewardType rewardType,      // 뽑혔을 때 지급할 보상 종류
        String     rewardCode,      // 보상 대상 코드 (재화형은 빈칸, ARTIFACT 빈칸이면 grade로 등급 랜덤
        GachaGrade grade, //ARTIFACT 가챠의 등급 랜덤용 필드 (rewardCode가 있으면 무시)
        long       amount,          // 지급 수량 (등급 랜덤 행은 1 고정, 횟수는 상품 보상 amount)
        long       weight           // 가중치 (확률 = weight / 같은 gachaCode의 weight 합계)
) {
    // 실제 아티팩트 등급 (NONE·빈칸 → null)
    public EquipmentGrade equipmentGrade() {
        return grade == null ? null : grade.toEquipmentGrade();
    }

    // 등급 랜덤 행: ARTIFACT + rewardCode 빈칸 + 등급 지정
    public boolean isRandomArtifact() {
        return rewardType == RewardType.ARTIFACT
                && (rewardCode == null || rewardCode.isBlank())
                && equipmentGrade() != null;
    }

    // 시트 데이터 → 지급용 DTO
    public Reward toReward() {
        return new Reward(rewardType, rewardCode, amount);
    }
}