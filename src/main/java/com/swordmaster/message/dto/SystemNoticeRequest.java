package com.swordmaster.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemNoticeRequest(
        @NotBlank(message = "공지 내용을 입력해주세요.")
        @Size(max = 500, message = "공지는 최대 500자입니다.")
        String content
) {}
