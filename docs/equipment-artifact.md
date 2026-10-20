# equipment.artifact

아티팩트의 강화, 장착, 해제와 강화 재료 지급을 담당, 장착 슬롯은 3개

소스 코드: [src/main/java/com/swordmaster/equipment/artifact](../src/main/java/com/swordmaster/equipment/artifact)

## 클래스

| 클래스 | 역할 |
| --- | --- |
| `ArtifactController` | API 진입점 |
| `ArtifactService` | 시작 아티팩트 지급, 조회, 강화, 장착·해제, 강화 재료 지급(`grantMaterialCount`) |
| `ArtifactTable`, `ArtifactStatTable`, `ArtifactEnhanceTable` | 아티팩트, 능력치, 강화 비용 시트 로딩·검증. `ArtifactStatTable`은 아티팩트 시트와의 관계도 검증 |

## 테이블: player_artifacts

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `player_id` | BIGINT | NOT NULL, FK → `players.id` | |
| `code` | VARCHAR(255) | NOT NULL | `artifacts.code` |
| `level` | INT | NOT NULL | 1부터 (획득 시 1) |
| `material_count` | INT | NOT NULL | 보유 강화 재료 |
| `equip_slot` | INT | | 1~3, `NULL`이면 미장착 |
| `version` | BIGINT | NOT NULL | 낙관적 락 |

유니크 `uk_player_artifacts_player_slot` (`player_id`, `equip_slot`) — 한 슬롯에 두 개가 장착되지 않게 막음, `NULL`끼리는 겹쳐도 가능

## 시트: artifacts

| 컬럼 | 타입 | 규칙 |
| --- | --- | --- |
| `code` | String | PK, 비어 있으면 안 됨 |
| `name` | String | 비어 있으면 안 됨 |
| `grade` | `EquipmentGrade` | `COMMON`, `RARE`, `EPIC`, `LEGENDARY` |

## 시트: artifact_stats

아티팩트 하나가 능력치를 여러 개 가질 수 있도록 별도 시트로 분리

| 컬럼 | 타입 | 규칙 |
| --- | --- | --- |
| `code` + `statType` | String, `EquipmentStat` | 복합 PK. `code`는 `artifacts.code`에 있어야 함. `statType`은 `CRIT_RATE`, `LIFESTEAL`, `MAX_HP`, `ATTACK_SPEED` |
| `baseValue` | double | 0 이상, 기본 능력치 |
| `valuePerLevel` | double | 0 이상, 레벨당 증가량 |

- 모든 아티팩트는 능력치를 1개 이상 가져야 함, 능력치가 없는 아티팩트나 `artifacts`에 없는 `code`가 있으면 서버가 시작되지 않음
- 능력치 계산은 전투 계산기(`CombatStatCalculator`) 기준

## 시트: artifact_enhance

| 컬럼 | 타입 | 규칙 |
| --- | --- | --- |
| `grade` + `level` | `EquipmentGrade`, int | 복합 PK. `level`은 강화 전 현재 레벨 (이 레벨에서 다음 레벨로 올리는 비용) |
| `materialCount` | int | 0 이상, 필요한 재료 수 |
| `gold` | long | 0 이상, 필요한 골드 |

## API

| 메서드 | 경로 | 요청 | 응답 |
| --- | --- | --- | --- |
| POST | `/api/artifacts/{code}/enhance` | `{ requestId }` | `ArtifactEnhanceResponse` |
| PUT | `/api/artifacts/{code}/slots/{slot}` | — | 장착 목록 배열 |
| DELETE | `/api/artifacts/slots/{slot}` | — | 장착 목록 배열 |

```json
// ArtifactEnhanceResponse
{ "artifactResponses": [ { "code": "CRIT_01", "level": 2, "materialCount": 0, "equipSlot": 1 } ],
  "currenciesResponse": { "gold": 4500, "diamond": 0 } }

// 장착·해제 응답 (장착 중인 것만, 슬롯 순)
[ { "code": "CRIT_01", "level": 2, "materialCount": 0, "equipSlot": 1 } ]
```

## 규칙

- 강화: 현재 레벨의 강화 비용 행이 시트에 없으면 최고 레벨, 재료와 골드를 함께 차감하고 레벨을 1 증가
- 장착: 이미 그 슬롯이면 변경 없음, 다른 슬롯에 있던 것이면 원래 슬롯에서 해제 후 이동, 요청 슬롯에 있던 다른 아티팩트는 해제, 해제는 즉시 DB에 반영(`saveAndFlush`)해 유니크 제약 충돌을 방지
- 강화·장착·해제 후 전투력을 랭킹에 기록

| 상태 | `code` | 상황 |
| --- | --- | --- |
| 400 | `INVALID_SLOT` | 슬롯이 1~3이 아님 |
| 400 | `ARTIFACT_MAX_LEVEL` | 이미 최고 레벨 |
| 400 | `NOT_ENOUGH_MATERIAL` | 강화 재료 부족 |
| 400 | `NOT_ENOUGH_GOLD` | 강화 골드 부족 |
| 404 | `ARTIFACT_NOT_OWNED` | 보유하지 않은 아티팩트 |
