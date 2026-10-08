package com.swordmaster.reward.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.reward.Reward;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final CurrencyService currencyService;

    public void reward(Player player, Reward reward, CurrencyReason reason) {
        switch (reward.rewardType()) {
            case GOLD: goldReward(player, reward.amount(),reason); break;

            default : throw new BusinessException(HttpStatus.NOT_IMPLEMENTED ,"지급처리가 구현되지 않은 보상입니다."+reward.rewardType());
        }
    }

    private void goldReward(Player player, long amount, CurrencyReason reason) {
        currencyService.grant(player, CurrencyType.GOLD,amount,reason);
    }
}
