# idempotency

같은 요청이 두 번 처리되지 않게 방지, 응답을 받기 전에 연결이 끊긴 클라이언트가 같은 `requestId`로 다시 보내면, 처음 응답을 그대로 반환

소스 코드: [src/main/java/com/swordmaster/idempotency](../src/main/java/com/swordmaster/idempotency)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `IdempotencyService` | `execute(userId, requestId, 응답 타입, 처리)`: 저장된 응답이 있으면 반환, 없으면 처리 후 응답 저장 |
| `IdempotencyKeyCleanupJob` | 매일 01:00 하루 지난 키 삭제 |

## 테이블: idempotency_keys

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | NOT NULL | 외래 키 없이 ID만 저장 |
| `request_id` | VARCHAR(255) | NOT NULL | 클라이언트가 보낸 UUID |
| `response_body` | TEXT | NOT NULL | 처음 처리한 응답 (JSON) |
| `created_at` | DATETIME(6) | NOT NULL | UTC |

유니크 `uk_idempotency_keys_user_request` (`user_id`, `request_id`), 인덱스 `idx_idempotency_keys_created_at`.

## 규칙

- 쓰는 곳: 검 강화·판매, 아티팩트 강화.
- 호출한 서비스의 트랜잭션 안에서 실행됨, 같은 키가 동시에 들어오면 나중 저장이 유니크 제약에 걸려 그 트랜잭션 전체가 취소되고 409로 응답
