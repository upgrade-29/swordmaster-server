# common

공통 예외와 오류 응답, 구글 시트 로딩을 담당

소스 코드: [src/main/java/com/swordmaster/common](../src/main/java/com/swordmaster/common)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `BusinessException`, `ErrorCode` | 상태·메시지·코드를 담는 업무 예외. 패키지별 `ErrorCode` 열거형으로 생성 |
| `GlobalExceptionHandler` | 모든 예외를 `{ message, code }` 형식으로 변환 |
| `GoogleSheetClient` | 시트를 CSV로 내려받아 행 목록으로 변환 (숫자는 Long·Double로) |
| `SheetTable<T>` | 시트 행을 레코드로 바꾸고 검증. 실패하면 서버가 시작되지 않음 |
| `ConstantTable`, `ConstantKey`, `ConstantType` | 상수 시트. 키마다 타입이 정해져 있고, 다른 타입으로 꺼내면 예외 |

## 시트: constants

| 컬럼 | 설명 |
| --- | --- |
| `key` | `ConstantKey` 이름 (유일) |
| `value` | 값 |
| `description` | 설명 |

| 키 | 타입 | 쓰는 곳 |
| --- | --- | --- |
| `INITIAL_GOLD` | LONG | 플레이어 생성 시 초기 골드 |
| `STARTER_ARTIFACTS` | TEXT_LIST | 플레이어 생성 시 지급할 아티팩트 코드 (쉼표 구분) |
| `CRIT_MULTIPLIER` | DECIMAL | 전투력 계산 |
| `BASE_BATTLE_TIME` | LONG | 전투력 계산 |
| `NORMAL_BATTLE_TIME_LIMIT`, `BOSS_BATTLE_TIME_LIMIT`, `HEAL_RATE` | LONG, LONG, DECIMAL | 전투 |
| `NOTICE_SWORD_LEVEL`, `NOTICE_DESTROY_LEVEL` | INT | 공지 기준 |
| `X_GAME_DATA_VERSION` | TEXT | 게임 데이터 버전 |

`ConstantKey`에 있는 키가 시트에 없거나 타입이 맞지 않으면 서버가 시작되지 않음, 키를 추가할 때는 시트에 먼저 추가해야 함

## 오류 응답

```json
{ "message": "골드가 부족합니다.", "code": "NOT_ENOUGH_GOLD" }
```

`code`는 클라이언트가 이유에 따라 처리해야 할 때만 존재, 업무 오류 코드는 각 패키지 문서 참조

| 상태 | 상황 |
| --- | --- |
| 400 | 본문 검증 실패, JSON 형식 오류, 파라미터 누락·타입 불일치 |
| 401 | 토큰 없음·만료·위조 (인증 필터) |
| 404 | 존재하지 않는 URL |
| 405 · 415 | 잘못된 HTTP 메서드, `Content-Type` 누락 |
| 409 | 낙관적 락 충돌, 유니크 제약 위반 (동시·중복 요청) |
| 500 | 예상치 못한 서버 오류 |
