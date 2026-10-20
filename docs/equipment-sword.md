# equipment.sword

검 강화와 판매를 담당, 검 단계는 `players.sword_level`에 저장하고, 단계별 값은 시트에서 로드

소스 코드: [src/main/java/com/swordmaster/equipment/sword](../src/main/java/com/swordmaster/equipment/sword)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `SwordController` | API 진입점 |
| `SwordService` | 강화(확률 판정, 골드 차감, 단계 변경, 랭킹 기록), 판매(첫 단계로 초기화, 골드 지급) |
| `SwordTable` | 검 시트 로딩·검증, 단계로 조회 |

## 시트: swords

| 컬럼 | 타입 | 규칙 |
| --- | --- | --- |
| `level` | int | PK. 0부터 (행 수 − 1)까지 빠짐없이 |
| `name` | String | 유일, 비어 있으면 안 됨 |
| `successRate` | Double | 0~1, 마지막 단계는 비워 둠 |
| `enhanceCost` | Long | 0 이상, 마지막 단계는 비워 둠 |
| `sellPrice` | long | 0 이상 |
| `attackPower`, `attackSpeed`, `maxHp` | long, double, long | 0 이상 (전투·전투력 계산) |
| `code` | String | 비어 있으면 안 됨 |

## API

| 메서드 | 경로 | 요청 | 응답 |
| --- | --- | --- | --- |
| POST | `/api/swords/enhance` | `{ requestId, expectedLevel }` | `SwordEnhanceResponse` |
| POST | `/api/swords/sell` | `{ requestId, expectedLevel }` | `SwordSellResponse` |

```json
// SwordEnhanceResponse
{ "result": "SUCCESS", "swordResponse": { "level": 4 }, "currenciesResponse": { "gold": 4900, "diamond": 0 } }

// SwordSellResponse
{ "swordResponse": { "level": 0 }, "rewardResponses": [ { ... } ], "currenciesResponse": { "gold": 6100, "diamond": 0 } }
```

## 규칙

- 강화 검사 순서: 레벨 불일치 → 최고 단계 → 골드 부족, 성공하면 +1, 실패하면(`DESTROYED`) 첫 단계로 롤백
- 강화 후 검 최고 단계(`SWORD_LEVEL`, 더 높을 때만)와 전투력(`COMBAT_POWER`)을 랭킹에 기록, 판매 후에는 전투력만 기록
- 같은 `requestId`로 다시 보내면 처음 응답을 그대로 반환

| 상태 | `code` | 상황 |
| --- | --- | --- |
| 400 | `LEVEL_MISMATCH` | `expectedLevel`과 서버 단계가 다름 |
| 400 | `SWORD_MAX_LEVEL` | 이미 최고 단계 |
| 400 | `SWORD_NOT_SELLABLE` | 첫 단계 검 판매 |
| 400 | `NOT_ENOUGH_GOLD` | 강화 골드 부족 |
