package com.swordmaster.idempotency.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name              = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name        = "uk_idempotency_keys_user_request",
                columnNames = {"user_id", "request_id"}
        ),
        indexes = @Index(
                name       = "idx_idempotency_keys_created_at",
                columnList = "created_at"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false)
    private String requestId;       // 클라이언트가 보낸 UUID

    // 처음 처리할 때의 응답
    // 처리 후 응답을 보내는 중 연결이 끊겨 클라이언트가 같은 requestId로 다시 요청할 때, 이미 처리된 요청을 재반환
    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKey(Long userId, String requestId, String responseBody) {
        this.userId       = userId;
        this.requestId    = requestId;
        this.responseBody = responseBody;
    }
}
