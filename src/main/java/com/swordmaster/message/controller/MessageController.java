package com.swordmaster.message.controller;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.ErrorResponse;
import com.swordmaster.message.dto.PublicChatRequest;
import com.swordmaster.message.service.MessageService;
import com.swordmaster.user.dto.UserResponse;
import com.swordmaster.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    // Client: SEND /app/chat/public
    @MessageMapping("/chat/public")
    public void sendPublicChat(
            @Valid @Payload PublicChatRequest request,
            Principal principal) {

        Long userId = Long.valueOf(principal.getName());

        // Client가 보낸 발신자 정보를 사용하지 않는다.
        UserResponse user = userService.getMe(userId);

        String content = request.content().strip();

        if (content.isBlank()) {
            throw new BusinessException("채팅 내용을 입력해주세요.");
        }

        if (content.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessException(
                    "줄바꿈과 제어문자는 사용할 수 없습니다."
            );
        }

        messageService.deliverPublicMessage(
                user.id(),
                user.nickname(),
                content
        );
    }

    // 오류는 송신한 연결에만 전달한다.
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleBusiness(BusinessException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException exception) {

        return new ErrorResponse(
                "채팅 내용은 공백이 아닌 1~500자로 입력해주세요."
        );
    }

    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleConversion(
            MessageConversionException exception) {

        return new ErrorResponse("채팅 요청 JSON 형식이 올바르지 않습니다.");
    }
}
