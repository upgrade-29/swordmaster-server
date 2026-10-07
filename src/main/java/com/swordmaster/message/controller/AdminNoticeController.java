package com.swordmaster.message.controller;

import com.swordmaster.common.BusinessException;
import com.swordmaster.message.dto.SystemNoticeRequest;
import com.swordmaster.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Void> sendNotice(
            @Valid @RequestBody SystemNoticeRequest request) {

        String content = request.content().strip();

        if (content.isBlank()) {
            throw new BusinessException("공지 내용을 입력해주세요.");
        }

        messageService.sendSystemNotice(content);

        return ResponseEntity.noContent().build();
    }
}
