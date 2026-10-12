package com.swordmaster.equipment.sword.controller;

import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.equipment.sword.dto.SwordRequest;
import com.swordmaster.equipment.sword.dto.SwordSellResponse;
import com.swordmaster.equipment.sword.service.SwordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/swords")
@RequiredArgsConstructor
public class SwordController {
    private final SwordService swordService;

    @PostMapping("/enhance")
    public SwordEnhanceResponse enhance(
            @AuthenticationPrincipal Long         userId,
            @RequestBody @Valid      SwordRequest request) {
        return swordService.enhance(userId, request);
    }

    @PostMapping("/sell")
    public SwordSellResponse sell(
            @AuthenticationPrincipal Long         userId,
            @RequestBody @Valid      SwordRequest request) {
        return swordService.sell(userId, request);
    }
}
