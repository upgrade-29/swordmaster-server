package com.swordmaster.message.dto;

import com.swordmaster.common.enums.MessageType;
import java.time.Instant;

public record MessageResponse(
        MessageType type,
        Long senderId,
        String senderNickname,
        String content,
        Integer swordLevel,
        Instant sentAt
) {}
