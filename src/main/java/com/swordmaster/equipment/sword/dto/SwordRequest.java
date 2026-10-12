package com.swordmaster.equipment.sword.dto;

import jakarta.validation.constraints.NotNull;

public record SwordRequest(
        @NotNull(message = "요청 ID가 필요합니다.")    String  requestId,
        @NotNull(message = "예상 Level이 필요합니다.") Integer expectedLevel
) {
}
