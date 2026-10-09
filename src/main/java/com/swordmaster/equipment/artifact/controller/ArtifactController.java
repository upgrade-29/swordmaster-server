package com.swordmaster.equipment.artifact.controller;

import com.swordmaster.equipment.artifact.dto.ArtifactEnhanceRequest;
import com.swordmaster.equipment.artifact.dto.ArtifactEnhanceResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
public class ArtifactController {
    private final ArtifactService artifactService;

    @PostMapping("/{code}/enhance")
    public ArtifactEnhanceResponse enhance(
            @AuthenticationPrincipal Long                   userId,
            @PathVariable            String                 code,
            @RequestBody @Valid      ArtifactEnhanceRequest request) {
        return artifactService.enhance(userId, code, request);
    }

    @PutMapping("/{code}/slots/{slot}")
    public List<ArtifactResponse> equip(
            @AuthenticationPrincipal Long   userId,
            @PathVariable            String code,
            @PathVariable            int    slot
    ) {
        return artifactService.equip(userId, code, slot);
    }

    @DeleteMapping("/slots/{slot}")
    public List<ArtifactResponse> remove(
            @AuthenticationPrincipal Long userId,
            @PathVariable            int  slot
    ) {
        return artifactService.remove(userId, slot);
    }
}
