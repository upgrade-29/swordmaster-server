package com.swordmaster.shop.iap;

import com.swordmaster.shop.dto.ShopPurchaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IapController {
    private final IapService iapService;

    public record GoogleIapRequest(
            @NotBlank @Size(max = 30)  String productCode,
            @NotBlank @Size(max = 512) String purchaseToken     // 영수증 자체가 멱등성 키 역할
    ) {}

    @PostMapping("/api/shop/iap/google")
    public ResponseEntity<ShopPurchaseResponse> purchaseWithGoogle(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GoogleIapRequest request) {
        return ResponseEntity.ok(
                iapService.purchaseWithGoogle(userId, request.productCode(), request.purchaseToken()));
    }
}