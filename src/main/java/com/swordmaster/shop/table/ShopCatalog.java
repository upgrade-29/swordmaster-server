package com.swordmaster.shop.table;

import com.swordmaster.shop.dto.Reward;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ShopCatalog {
    private final ShopProductTable       products;
    private final ShopProductRewardTable rewards;

    public ShopCatalog(ShopProductTable products, ShopProductRewardTable rewards) {
        this.products = products;
        this.rewards  = rewards;

        validate();
    }

    // 검증
    private void validate() {
        List<String> errors = new ArrayList<>();

        // 보상이 가리키는 상품은 상품 시트에 존재해야 함
        for (String productCode : rewards.productCodes())
            if (products.findProduct(productCode).isEmpty())
                errors.add("ShopProduct에 없는 상품 코드: " + productCode);

        // 모든 상품에는 보상이 하나 이상 있어야 함
        for (ShopProduct product : products.getAll())
            if (rewards.findRewards(product.productCode()).isEmpty())
                errors.add("보상이 없는 상품: " + product.productCode());

        if (errors.isEmpty()) return;

        throw new IllegalStateException(
                "ShopCatalog 검증에 실패하였습니다:\n - " + String.join("\n - ", errors)
        );
    }

    // 검색
    public Optional<ShopProduct> findProduct(String productCode) {
        return products.findProduct(productCode);
    }

    public List<Reward> findRewards(String productCode) {
        return rewards.findRewards(productCode);
    }
}