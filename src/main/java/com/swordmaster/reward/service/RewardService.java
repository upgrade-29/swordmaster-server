package com.swordmaster.reward.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import com.swordmaster.gacha.service.GachaService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.reward.Reward;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final CurrencyService currencyService;
    private final GachaService gachaService;
    private final ArtifactService artifactService;

    public List<Reward> reward(Player player, Reward reward, CurrencyReason reason) {
        return switch (reward.rewardType()) {
            case GOLD -> {
                goldReward(player, reward.amount(), reason);
                yield List.of(reward);
            }

            case ARTIFACT -> {
                artifactReward(player,reward.rewardCode(),reward.amount());
                yield List.of(reward);
            }

            case GACHA -> {
                List<Reward> granted = new ArrayList<>();
                for (Reward drawn : gachaReward(reward.rewardCode(), reward.amount())) {
                    granted.addAll(reward(player, drawn, reason));   // 뽑힌 보상을 실제 지급
                }
                yield granted;
            }
            default -> throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,
                    "지급처리가 구현되지 않은 보상입니다." + reward.rewardType());
        };
    }

    private void goldReward(Player player, long amount, CurrencyReason reason) {
        currencyService.grant(player, CurrencyType.GOLD, amount, reason);
    }

    private void artifactReward(Player player,String code, long amount) {
        artifactService.grantMaterialCount(player,code, amount);
    }

    private List<Reward> gachaReward(String rewardCode, long amount) {
        return gachaService.open(rewardCode, amount);
    }
}
