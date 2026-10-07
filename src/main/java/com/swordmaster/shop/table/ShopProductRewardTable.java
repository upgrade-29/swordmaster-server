package com.swordmaster.shop.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.shop.RewardType;
import com.swordmaster.shop.dto.Reward;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShopProductRewardTable extends SheetTable<ShopProductReward> {
    // 상품 코드별 보상 목록 (시트 행 순서 유지)
    private final Map<String, List<ShopProductReward>> byProduct;

    public ShopProductRewardTable(GoogleSheetClient sheets, @Value("${game-data.gid.shopproductreward}") long gid) {
        super(sheets, gid, ShopProductReward.class);    // 변환 후 검증까지 실행

        byProduct = rows.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(ShopProductReward::productCode, Collectors.toUnmodifiableList()),
                        Map::copyOf
                ));
    }

    // 검증 (이 테이블 혼자 확인 가능한 것만, 상품 테이블과의 교차 검증은 ShopCatalog)
    @Override
    protected void validate(List<String> errors) {
        for (ShopProductReward reward : rows) {
            // productCode는 Not Blank
            if (reward.productCode() == null || reward.productCode().isBlank()) {
                errors.add("productCode가 비어 있는 보상 행이 있습니다.");

                continue;
            }

            String at = reward.productCode() + "의 보상 ";

            // 보상 종류는 Not Null
            if (reward.rewardType() == null) {
                errors.add(at + "rewardType이 비어 있습니다.");

                continue;
            }

            // 지급 수량은 0보다 커야 함
            if (reward.amount() <= 0) errors.add(at + "amount는 0보다 커야 합니다.");

            // GOLD는 rewardCode 없음, 나머지는 rewardCode 필수
            boolean noCode = reward.rewardCode() == null || reward.rewardCode().isBlank();

            if (reward.rewardType() == RewardType.GOLD && !noCode)
                errors.add(at + "GOLD는 rewardCode를 비워야 합니다.");
            if (reward.rewardType() != RewardType.GOLD && noCode)
                errors.add(at + reward.rewardType() + "는 rewardCode가 필요합니다.");
        }
    }

    // 검색
    // 보상이 존재하는 상품 코드 목록 (교차 검증용)
    public Set<String> productCodes() {
        return byProduct.keySet();
    }

    // 상품 코드의 보상 목록을 응답용 DTO로 변환
    public List<Reward> findRewards(String productCode) {
        return byProduct.getOrDefault(productCode, List.of()).stream()
                .map(ShopProductReward::toReward)
                .toList();
    }
}