package com.swordmaster.reward;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final CurrencyService currencyService;

    public void reward(Long userId, Reward reward) {
        switch (reward.rewardType()) {
            case GOLD: goldReward(userId, reward.amount()); break;
            case ITEM: break;
            case ARTIFACT:break;
            case GACHA_BOX:break;
            case PROFILE_IMAGE :break;
            default : throw new BusinessException(HttpStatus.CONFLICT,
                    "지급 처리가 구현되지 않은 보상 종류입니다: " + reward.rewardType());
        }
    }

    private void goldReward(Long userId, long amount){
        currencyService.grant(userId, CurrencyType.GOLD,amount);
    }
}
