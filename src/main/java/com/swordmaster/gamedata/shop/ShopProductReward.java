package com.swordmaster.gamedata.shop;


import com.swordmaster.shop.RewardType;

//구글 스프레드 시트의 ShopProductReward 데이터의 열 (구입 시 일치하는 product_code 에 대한 보상의 관한 데이터)
public record ShopProductReward(
        String productCode,
        RewardType rewardType,
        String rewardCode,
        long amount
) {}