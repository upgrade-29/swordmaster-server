package com.swordmaster.gacha.table;

import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;

public record GachaEntry(
        String     gachaCode,       // 가챠(상자) 코드 — ShopProductReward의 GACHA_BOX rewardCode와 연결
        RewardType rewardType,      // 뽑혔을 때 지급할 보상 종류
        String     rewardCode,      // 보상 대상 코드 (GOLD는 빈칸 → null)
        long       amount,          // 지급 수량
        long       weight           // 가중치 (확률 = weight / 같은 gachaCode의 weight 합계)
) {
    // 시트 데이터 → 지급용 DTO
    public Reward toReward() {
        return new Reward(rewardType, rewardCode, amount);
    }
}