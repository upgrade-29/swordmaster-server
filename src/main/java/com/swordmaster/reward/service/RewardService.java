package com.swordmaster.reward.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.reward.Reward;
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

            default : throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"지급처리가 구현되지 않은 보상입니다.");
        }
    }

    private void goldReward(Long userId, long amount){
        currencyService.grant(userId, CurrencyType.GOLD,amount);
    }
}
