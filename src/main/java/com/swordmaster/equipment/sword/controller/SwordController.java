package com.swordmaster.equipment.sword.controller;

import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.equipment.sword.service.SwordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment/sword")
@RequiredArgsConstructor
public class SwordController {
    private final SwordService swordService;

    @PostMapping("/enhance")
    public SwordEnhanceResponse enhance(@AuthenticationPrincipal Long userId) {
        return swordService.enhance(userId);
    }
}
