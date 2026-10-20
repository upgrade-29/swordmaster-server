# player

플레이어 생성·조회와 검 단계, 클리어 스테이지를 관리, 대부분의 서비스에 이용(조회)되는 기반 기능

소스 코드: [src/main/java/com/swordmaster/player](../src/main/java/com/swordmaster/player)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `PlayerController` | API 진입점 |
| `PlayerFacade` | 생성·조회 시 재화, 아티팩트를 함께 묶음 (순환 참조 방지) |
| `PlayerService` | 플레이어 조회·생성, 검 단계 변경, 스테이지 조건부 갱신, 닉네임 일괄 조회 |

## 테이블: players

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | NOT NULL, UK, FK → `users.id` | 유저당 하나 |
| `nickname` | VARCHAR(10) | NOT NULL, UK `uk_players_nickname` | |
| `sword_level` | INT | NOT NULL | 현재 검 단계 |
| `cleared_stage` | INT | NOT NULL, 기본값 0 | 클리어한 최고 스테이지 |

`@DynamicUpdate`로 바뀐 컬럼만 UPDATE, `cleared_stage`는 조건부 UPDATE 쿼리(더 높을 때만)로 바뀌므로, 같은 요청 안에서 메모리의 `Player`에는 반영되지 않음

## API

| 메서드 | 경로 | 요청 | 응답 |
| --- | --- | --- | --- |
| GET | `/api/players/me` | — | `PlayerResponse` |
| POST | `/api/players` | `{ "nickname": "검객" }` | `PlayerResponse` (201) |

```json
{
  "userId": 1, "nickname": "검객",
  "swordResponse":      { "level": 0 },
  "currenciesResponse": { "gold": 5000, "diamond": 0 },
  "artifactResponses":  [ { "code": "CRIT_01", "level": 1, "materialCount": 0, "equipSlot": null } ],
  "stageProgress":      { "clearedStage": 0 }
}
```

- 닉네임: 공백 없이 1~10자
- 생성: 검 첫 단계로 플레이어 생성 → 재화 행 생성(초기 골드 지급) → 시작 아티팩트 지급

| 상태 | 상황 |
| --- | --- |
| 404 | 유저 정보 없음, 플레이어 정보 없음 |
| 409 | 플레이어가 이미 있음, 닉네임 중복, 동시 생성 요청 |
