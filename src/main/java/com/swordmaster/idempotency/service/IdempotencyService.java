package com.swordmaster.idempotency.service;

import com.swordmaster.idempotency.entity.IdempotencyKey;
import com.swordmaster.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final JsonMapper               jsonMapper;

    public <T> T execute(Long userId, String requestId, Class<T> responseType, Supplier<T> action) {
        Optional<IdempotencyKey> saved = idempotencyKeyRepository.findByUserIdAndRequestId(userId, requestId);

        // 이미 처리한 요청이면 저장된 응답을 반환
        if (saved.isPresent()) return jsonMapper.readValue(saved.get().getResponseBody(), responseType);

        // 처음 온 요청이면 처리 후 응답과 함께 저장
        T response = action.get();

        // 동시에 같은 키가 들어오면 중복 키 예외로 트랜잭션 전체 취소
        idempotencyKeyRepository.saveAndFlush(
                new IdempotencyKey(userId, requestId, jsonMapper.writeValueAsString(response))
        );

        return response;
    }
}
