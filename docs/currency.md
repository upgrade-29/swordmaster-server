# currency

재화의 지급·차감과 변동 기록 및 잔액 검증을 담당, API는 없고 다른 기능이 `CurrencyService`를 호출

소스 코드: [src/main/java/com/swordmaster/currency](../src/main/java/com/swordmaster/currency)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `CurrencyService` | `createAll`(생성 시 모든 재화 행 + 초기 골드), `getAll`, `grant`(지급), `consume`(차감) |
| `CurrencyBackfillRunner` | 서버 시작 시 새 재화 종류가 있으면 모든 플레이어에게 0으로 행 추가 |
| `CurrencyReconciliationJob` | 매일 01:00 잔액과 기록 합계를 비교해 다른 행을 로그로 남김 |

## 테이블: player_currencies

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `player_id` | BIGINT | NOT NULL, FK → `players.id` | |
| `type` | VARCHAR(20) | NOT NULL | `GOLD`, `DIAMOND` |
| `amount` | BIGINT | NOT NULL | 현재 잔액 |
| `version` | BIGINT | NOT NULL | 낙관적 락 |

유니크 `uk_player_currencies_player_type` (`player_id`, `type`).

## 테이블: currency_histories

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `player_id` | BIGINT | NOT NULL | 외래 키 없이 ID만 저장 |
| `type` | VARCHAR(20) | NOT NULL | 재화 종류 |
| `amount` | BIGINT | NOT NULL | 변동량 (차감은 음수) |
| `balance_after` | BIGINT | NOT NULL | 변동 후 잔액 |
| `reason` | VARCHAR(20) | NOT NULL | `PLAYER_CREATE`, `EQUIP_ENHANCE`, `EQUIP_SELL`, `PURCHASE`, `REWARD`, `ADMIN_ADJUST` |
| `memo` | VARCHAR(255) | | 관리자 조정 사유 |
| `created_at` | DATETIME(6) | NOT NULL | UTC |

인덱스 `idx_currency_histories_player` (`player_id`, `type`, `id`). `@Immutable`이라 추가만 가능

## 규칙

- 변동량이 0이면 아무것도 바꾸지 않고 현재 재화를 돌려주며, 기록도 남기지 않음, 음수는 코드 오류로 500
- 지급·차감마다 변동량과 변동 후 잔액을 기록, 응답은 바뀐 뒤의 모든 재화
- 재화·사유 열거형 컬럼은 `@JdbcTypeCode(SqlTypes.VARCHAR)`로 VARCHAR에 저장해, 종류를 추가해도 컬럼을 고칠 필요가 없음

| 상태 | `code` | 상황 |
| --- | --- | --- |
| 400 | `NOT_ENOUGH_GOLD`, `NOT_ENOUGH_DIAMOND` | 잔액 부족 |
| 500 | — | 변동량이 음수, 재화 행이 없음 |
