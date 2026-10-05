package com.swordmaster.shop.dto;

import com.swordmaster.shop.RewardType;

//보상 관련해서 데이터를 옮기는 목적의 레코드 , 이후 Reward 항목이 나왔을 때 리팩토링
public record Reward(
        RewardType rewardType,
        String rewardCode,
        long amount) {}