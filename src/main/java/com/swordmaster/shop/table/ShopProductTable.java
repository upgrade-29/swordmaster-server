package com.swordmaster.shop.table;

import com.swordmaster.common.sheet.GoogleSheetClient;
import com.swordmaster.common.sheet.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ShopProductTable extends SheetTable<ShopProduct> {
    // 상품 코드로 조회
    private final Map<String, ShopProduct> byCode;

    public ShopProductTable(GoogleSheetClient sheets, @Value("${game-data.gid.shopproduct}") long gid) {
        super(sheets, gid, ShopProduct.class);      // 변환 후 검증까지 실행

        // 키 조회용 (validate에서 중복이 걸러진 뒤라 키 충돌 없음)
        byCode = rows.stream()
                .collect(Collectors.toUnmodifiableMap(ShopProduct::productCode, Function.identity()));
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // productCode는 중복 불가
        checkUnique("productCode", ShopProduct::productCode, errors);

        for (ShopProduct product : rows) {
            // productCode는 Not Blank (이후 메시지의 기준이므로 먼저 검사)
            if (isBlank(product.productCode())) {
                errors.add("productCode가 비어 있는 행이 있습니다.");

                continue;
            }

            String at = product.productCode() + "의 ";

            // 나머지 문자열 컬럼은 Not Blank
            if (isBlank(product.name()))     errors.add(at + "name이 비어 있습니다.");
            if (isBlank(product.category())) errors.add(at + "category가 비어 있습니다.");
            if (isBlank(product.iconCode())) errors.add(at + "iconCode가 비어 있습니다.");

            // 결제 재화는 Not Null
            if (product.priceType() == null) errors.add(at + "priceType이 비어 있습니다.");

            // 가격은 0보다 커야 함 (무료 상품 허용 여부는 기획 확인 필요)
            if (product.price() <= 0) errors.add(at + "price는 0보다 커야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 검색
    // 상품 코드는 클라이언트 입력이므로 없을 수 있음 → Optional
    public Optional<ShopProduct> findProduct(String productCode) {
        return Optional.ofNullable(byCode.get(productCode));
    }
}