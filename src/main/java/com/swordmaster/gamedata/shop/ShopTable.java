package com.swordmaster.gamedata.shop;

import java.util.*;
import java.util.stream.Collectors;

/**
 * shopProducts + shopProductRewards 시트.
 * rows: 시트 순서 그대로 (GET /api/game-data 응답용)
 * map : 코드로 바로 찾기 (구매 처리용)
 */
public record ShopTable(
        List<ShopProduct> productRows, //원래 순서 그대로의 상품 시트
        List<ShopProductReward> rewardRows, //원래 순서 그대로의 보상 시트
        Map<String, ShopProduct> products, //상품 코드 순으로 상품
        Map<String, List<ShopProductReward>> rewards //상품 코드 별로 나눈 보상 목록
) {

    public static ShopTable of(List<ShopProduct> productRows, List<ShopProductReward> rewardRows) {
        // 상품코드가 같은 상품이 List에 존재하는 지 검사 (같은 상품이 존재한다면 예외 발생)
        // map.put()은 이미 동일한 키가 존재한다면 원래 있던 값을 돌려준다. (만약 키가 없었다면 null 반환)
        Map<String, ShopProduct> productMap = new HashMap<>();
        for (ShopProduct product : productRows) {
            if (productMap.put(product.productCode(), product) != null) {
                throw new IllegalStateException("중복된 상품 코드: " + product.productCode());
            }
        }

        // 보상 목록 중에 상품 테이블의 상품코드와 일치하지 않는 항목을 검사 (일치하지 않는 항목이 있다면 예외 발생)
        for (ShopProductReward reward : rewardRows) {
            if (!productMap.containsKey(reward.productCode())) {
                throw new IllegalStateException("shopProducts에 없는 상품 코드: " + reward.productCode());
            }
        }

        // 키 상품코드, 값 보상 목록으로 상품코드에 별로 보상목록들을 묶은 맵으로 만듬
        Map<String, List<ShopProductReward>> rewardMap = rewardRows.stream()
                .collect(Collectors.groupingBy(ShopProductReward::productCode,
                        Collectors.toUnmodifiableList()));

        // 모든 상품에 보상이 존재하는 지 대조하여 확인하고 검증되었다면 맵으로 만듬
        for (String productCode : productMap.keySet()) {
            if (!rewardMap.containsKey(productCode)) {
                throw new IllegalStateException("보상이 없는 상품: " + productCode);
            }
        }

        return new ShopTable(
                List.copyOf(productRows),
                List.copyOf(rewardRows),
                Map.copyOf(productMap),
                Map.copyOf(rewardMap));
    }

    public Optional<ShopProduct> findProduct(String productCode) {
        return Optional.ofNullable(products.get(productCode));
    }

    public List<ShopProductReward> findRewards(String productCode) {
        return rewards.getOrDefault(productCode, List.of());
    }
}