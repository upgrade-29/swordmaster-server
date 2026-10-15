package com.swordmaster.ranking.controller;

import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.dto.RankingResponse;
import com.swordmaster.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {
    private final RankingService rankingService;

    @GetMapping
    public RankingResponse getRanking(
            @AuthenticationPrincipal Long        userId,
            @RequestParam            RankingType type
    ) {
        return rankingService.getRanking(userId, type);
    }
}
