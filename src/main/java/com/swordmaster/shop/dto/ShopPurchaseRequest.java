package com.swordmaster.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

//구매 버튼 클릭시 요청하는 DTO
public record ShopPurchaseRequest(
        @NotNull UUID requestId,                     // 중복 요청 방지 (재전송 시 같은 값)
        @NotBlank @Size(max = 30) String productCode // 상점 시트의 상품 코드
) {}