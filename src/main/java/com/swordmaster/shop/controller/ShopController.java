package com.swordmaster.shop.controller;


import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.facade.ShopFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop")
public class ShopController {
    private final ShopFacade shopFacade;

    @PostMapping("/purchase")
    public ResponseEntity<ShopPurchaseResponse> purchase(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ShopPurchaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shopFacade.purchase(userId,request));
    }

}
