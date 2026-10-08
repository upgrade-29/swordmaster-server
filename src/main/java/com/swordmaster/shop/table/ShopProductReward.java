package com.swordmaster.shop.table;

import com.swordmaster.reward.RewardType;
import com.swordmaster.reward.Reward;

public record ShopProductReward(
        String     productCode,     // 연결된 상품 코드
        RewardType rewardType,      // 보상 종류
        String     rewardCode,      // 보상 대상 코드 (GOLD는 빈칸 → null)
        long       amount           // 지급 수량
) {
    // 시트 데이터 → 응답·기록용 DTO
    public Reward toReward() {
        return new Reward(rewardType, rewardCode, amount);
    }
}