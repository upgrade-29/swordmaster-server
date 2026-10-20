# Swordmaster-Server

## 프로젝트 소개
2026 내일배움캠프 단기심화 최종프로젝트 서버

9조(강화이구조) 팀프로젝트 서버

[클라이언트 주소]
https://github.com/upgrade-29/swordmaster-client

## 개발기간
26.09.23 ~ 26.10.21 (기획기간 포함)

## 개발환경 및 기술 스택

- Intellij

    Spring Boot(4.1.1) (Java, JPA, MySQL, RDS)
  
    JDK : 21.amazon

- Docker

- Postman

- AWS

    123123

    123123
    

## 개발자 소개
- 김관영( https://github.com/UR-KGY ) : 상점 및 CICD
- 오수호( https://github.com/OHSUHO ) : 전투
- 이훈( https://github.com/Lee-Hun97 ) : 로그인 및 채팅
- 허준혁 ( https://github.com/slshan0597 ) : 플레이어 기반 DB 설계, 장비 강화, 랭킹 기록

## 주요 기능
<details>
  <summary>유지보수 및 배포</summary>
  <div markdown="1">

### 접속 주소

| 용도 | 주소 |
| --- | --- |
| REST API | `https://api.upgrade-29.com` |
| WebSocket (STOMP) | `wss://api.upgrade-29.com/ws` |
| 헬스체크 | `https://api.upgrade-29.com/actuator/health/readiness` |

- 도메인: 가비아 `upgrade-29.com`, `api` → ALB CNAME / 인증서: ACM `api.upgrade-29.com` (DNS 검증, 자동 갱신)
- ALB 기본 주소(`….elb.amazonaws.com`)로는 HTTPS 불가 (인증서 이름 불일치) → 항상 도메인 사용
- HTTP(80)는 클라이언트 전환 완료 전까지 임시 허용, 전환 후 차단 예정 (`ws://`는 리다이렉트를 따라가지 못함)
- 가비아의 **ACM 검증용 CNAME은 삭제 금지** (인증서 자동 갱신에 필요)

### 인프라 구조

```
사용자 ─443/80─▶ ALB(퍼블릭) ─8080─▶ EC2(프라이빗, ASG) ─3306─▶ RDS
                                                     └─6379─▶ ElastiCache
EC2 ──▶ NAT ──▶ ECR / SSM Parameter Store / Google Sheets
```

| 구성 | 내용 |
| --- | --- |
| 서버 | EC2 Auto Scaling Group(`swordmaster-asg`) + ALB, 시작 템플릿 user-data로 컨테이너 실행 |
| 이미지 | Docker `linux/arm64` → Amazon ECR, 태그 `vYYYYMMDD-HHmm-커밋7자리` (최근 10개 보관) |
| DB / 캐시 | RDS(MySQL 8, 단일 AZ), ElastiCache(Valkey, SSL) |
| 스키마 | Flyway 마이그레이션 + Hibernate `validate` |
| 설정·비밀값 | SSM Parameter Store `/swordmaster/prod/*` |
| 로그 | CloudWatch Logs `/swordmaster/prod/app` (스트림 = 인스턴스 ID), 초기화 로그 `/var/log/swordmaster-init.log` |

| 보안 그룹 | 인바운드 |
| --- | --- |
| `alb-sg` | 80, 443 ← 전체 |
| `app-sg` | 8080 ← `alb-sg` (22번 없음) |
| `rds-sg` | 3306 ← `app-sg`, `bastion-sg` |
| `valkey-sg` | 6379 ← `app-sg`, `bastion-sg` |

- RDS·ElastiCache는 프라이빗 → 로컬 PC에서 직접 접속 불가 (의도한 설계)
- 앱 서버 접속은 SSM Session Manager (키·22번 불필요)

### CI/CD

```mermaid
flowchart LR
    A[deploy-test / main push] --> B[bootJar 빌드]
    B --> C[GitHub OIDC로 AWS 로그인]
    C --> D[arm64 이미지 → ECR]
    D --> E[SSM IMAGE_TAG 갱신]
    E --> F[ASG Instance Refresh]
    F --> G[새 인스턴스 user-data<br/>Parameter Store → docker run]
    G --> H[Spring 기동<br/>Flyway → 시트 로드·검증]
    H --> I{ALB readiness}
    I -- healthy --> J[기존 인스턴스 교체]
    I -- unhealthy --> K[새 인스턴스 종료, 기존 유지]
```

- 새 인스턴스가 healthy가 되기 전까지 기존 인스턴스 유지 (`MinHealthyPercentage 100`, 워밍업 180초)
- **CI 성공 = 교체 요청 접수.** 실제 결과는 ASG *인스턴스 새로 고침*·*활동* 탭과 CloudWatch 로그로 확인
- 새 인스턴스는 부팅 시 `IMAGE_TAG`를 읽으므로, CI의 refresh 단계만 실패해도 이후 뜨는 인스턴스는 최신 이미지 사용
- Instance Refresh는 **한 번에 하나** → 진행 중이면 `InstanceRefreshInProgress`. 진행 중 refresh 완료/취소(`Cancelled` 확인) 후 재시작

### 운영 환경변수 (Parameter Store `/swordmaster/prod/`)

| 이름 | 용도 |
| --- | --- |
| `IMAGE_TAG` | 배포 이미지 태그 (CI가 갱신) |
| `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` | RDS 접속 |
| `REDIS_HOST` | ElastiCache 접속 |
| `JWT_SECRET` | JWT 서명 (교체 시 기존 토큰 모두 무효 → 재로그인) |
| `GOOGLE_WEB_CLIENT_ID` | 구글 로그인 토큰 검증 |

**환경변수 추가 절차** (하나라도 빠지면 부팅 실패)

1. Parameter Store에 `/swordmaster/prod/KEY` 생성 — 값은 **실제 값** (`${KEY}` 입력 금지)
2. 시작 템플릿 새 버전: user-data에 `KEY=$(retry getp KEY)` + `-e KEY="$KEY" \` 추가 → **기본 버전 지정** (ASG는 `Default` 사용)
3. `application-prod.properties`에 `KEY=${KEY}` 추가
4. 배포 후 새 인스턴스 태그 `aws:ec2launchtemplate:version`이 새 버전인지 확인

> user-data는 `set -euo pipefail` → 파라미터 하나라도 못 읽으면 컨테이너 실행 전에 중단

### 헬스체크

| 항목 | 값 |
| --- | --- |
| ALB 대상 그룹 경로 | `/actuator/health/readiness` |
| Security 허용 | `FilterConfig`의 `GET /actuator/health/**` permitAll |
| ASG 헬스체크 | ELB 기준, 유예 180초 |
| 등록 해제 지연 | 30초 |

- `/actuator/health`(DB·Redis 포함) 대신 readiness를 쓰는 이유: DB·Redis 일시 장애 시 **멀쩡한 서버까지 교체가 반복**되는 연쇄 장애 방지. 외부 의존성 장애는 5xx 알람으로 감지
- 경로 변경 순서: ① 새 경로 허용 코드 배포 → ② 새 경로 200 직접 확인 → ③ 대상 그룹 경로 변경 (순서가 바뀌면 401 → 전 서버 Unhealthy → 교체 반복)

### 모니터링·알림

| 알람 | 조건 | 의미 |
| --- | --- | --- |
| `swordmaster-no-healthy-host` | Healthy 대상 0대, 2분 | 서비스 중단 |
| `swordmaster-target-5xx` | 앱 5xx 5분간 5건 이상 | 앱 에러 (DB·Redis 장애 포함) |
| `swordmaster-rds-low-storage` | RDS 남은 공간 2GB 미만 | 디스크 부족 임박 |

- 모두 SNS `swordmaster-alerts` → 이메일 (경보·복구 둘 다)
- 로그 검색어: `ERROR`, `Exception`, `APPLICATION FAILED`, `Started`, `Migrating`

### 운영 규칙

- **마이그레이션 파일명** `VyyyyMMdd_HHmm__설명.sql` (작성 시점). 병합 시 develop 최신 버전보다 작으면 운영 Flyway validate 실패 → 파일명 재조정
- **시트 컬럼·값 변경 순서: 코드 배포 → 시트 변경.** 서버는 부팅 시 시트를 엄격 검증하므로 코드가 모르는 열·값이 있으면 기동 중단
- **Parameter Store·시트는 부팅 시 한 번만 읽음** → 값만 바꾸면 실행 중 서버에 미반영, 아래 재배포 필요
- **마이그레이션이 포함된 배포는 이미지 롤백 금지** → 수정 커밋으로 재배포

### DB 접근·데이터 보호

| 항목 | 내용 |
| --- | --- |
| 삭제 방지 | 켜짐 |
| 자동 백업 | 1일 (프리 플랜 상한) |
| 수동 스냅샷 | 위험 작업 직전 `before-작업-날짜` (컬럼·테이블 삭제 마이그레이션 배포 전 필수) |
| 접근 방법 | 바스티온 SSH 터널(`127.0.0.1:3307`) 또는 SSM 포트 포워딩 |
| 팀원 계정 | 조회 전용 계정 별도 발급 (`GRANT SELECT ON swordmaster.*`) |

**배포 실패 시 확인 순서:** ASG 활동 탭 → 새 인스턴스 태그(템플릿 버전) → CloudWatch 해당 인스턴스 스트림 → (스트림 없으면) Session Manager로 `/var/log/swordmaster-init.log`

### 현재 제약

| 제약 | 이유 | 대응 |
| --- | --- | --- |
| 서버 1대 고정 (스케일링 정책 비활성) | STOMP `SimpleBroker`가 서버 메모리 → 2대면 채팅이 갈림 | 외부 브로커(Redis Pub/Sub 등) 적용 후 재활성화 |
| 배포 중 WebSocket 끊김 | 인스턴스 교체 (드레이닝 30초) | 클라이언트 자동 재연결 |
| RDS 단일 AZ | 비용 | 출시 시 Multi-AZ 검토 |
| `deploy-test`·`main`이 같은 ASG에 배포 | 임시 운영 | main 배포 안정화 후 deploy-test 트리거 제거 |
  </div>
</details>

<details>
  <summary>인증과 인가</summary>
  <div markdown="1">
    
...  
회원가입과 로그인, WebSocket의 통신을 제외한 모든 요청에는 JWT인증이 필요하다.

로그인 성공 시 서버는 클라이언트에게 **JWT**(AccessToken + RefreshToken)를 반환해주고, (RefreshToken 미구현)

클라이언트가 서버의 api를 호출하는 경우 **filter기능**을 통해서 JWT를 통한 인증을 먼저 거치게된다.

AccessToken 만료 직전 클라이언트는 서버에 재연결을 요청하여 새로운 JWT를 전달받아야한다. (재연결 미구현)

모바일 환경이기에 만료 직전 클라이언트가 가동을 안하는 경우를 대비해, 재연결 시 만료 직전 시간을 비교하여 재연결을 요청해야한다.

**WebSocket** 통신의 경우 처음 **STOMP connect**에만 JWT가 필요하고, AccessToken 만료 직전 재발급 시 자동으로 재연결을 요청해야한다.
  </div>
</details>

<details>
  <summary>허준혁 - 플레이어의 기반 DB(재화 등) 설계, 장비(검, 아티팩트) 강화, 랭킹 기록</summary>
  <div markdown="1">

| 기능 | 구현 내용 | 상세 |
| --- | --- | --- |
| 공통 | 패키지 구조 설계, `ErrorCode` 기반 일관된 오류 응답, 구글 시트 게임 데이터 로딩·검증, 타입이 정해진 상수 테이블 | [common](docs/common.md) |
| 플레이어 | 플레이어 생성·조회, 닉네임 중복과 동시 생성 방지 | [player](docs/player.md) |
| 재화 | 골드·다이아 지급·차감, 모든 변동을 잔액과 함께 기록, 매일 잔액과 기록 합계 검증 | [currency](docs/currency.md) |
| 검 | 확률 강화(성공 +1, 실패 시 파괴)와 판매, 요청 레벨 검증 | [equipment-sword](docs/equipment-sword.md) |
| 아티팩트 | 재료·골드 강화, 3개 슬롯 장착·해제·이동, 능력치 시트 분리 | [equipment-artifact](docs/equipment-artifact.md) |
| 멱등성 | `requestId`로 같은 요청의 중복 처리 방지, 동시 요청은 유니크 제약으로 차단 | [idempotency](docs/idempotency.md) |
| 랭킹 | 검 최고 단계·스테이지·전투력 랭킹. DB 원본 + Redis 실시간 랭킹·일일 집계본, 집계본 기준 예상 순위 | [ranking](docs/ranking.md) |

**ERD**
 
```mermaid
erDiagram
    users ||--|| players : "user_id"
    players ||--o{ player_currencies : "player_id"
    players ||--o{ player_artifacts : "player_id"
    players ||--o{ player_rankings : "player_id"
    players ||..o{ currency_histories : "player_id (FK 없음)"
    users ||..o{ idempotency_keys : "user_id (FK 없음)"
 
    users {
        BIGINT id PK
    }
    players {
        BIGINT id PK
        BIGINT user_id FK, UK
        VARCHAR nickname UK
        INT sword_level
        INT cleared_stage
    }
    player_currencies {
        BIGINT id PK
        BIGINT player_id FK
        VARCHAR type "UK(player_id, type)"
        BIGINT amount
        BIGINT version
    }
    currency_histories {
        BIGINT id PK
        BIGINT player_id
        VARCHAR type
        BIGINT amount
        BIGINT balance_after
        VARCHAR reason
        VARCHAR memo
        DATETIME created_at
    }
    player_artifacts {
        BIGINT id PK
        BIGINT player_id FK
        VARCHAR code
        INT level
        INT material_count
        INT equip_slot "UK(player_id, equip_slot)"
        BIGINT version
    }
    player_rankings {
        BIGINT id PK
        BIGINT player_id FK
        VARCHAR type "UK(player_id, type)"
        BIGINT score
        DATETIME achieved_at
    }
    idempotency_keys {
        BIGINT id PK
        BIGINT user_id
        VARCHAR request_id "UK(user_id, request_id)"
        TEXT response_body
        DATETIME created_at
    }
```
 
- 실선은 외래 키, 점선은 ID만 저장하는 관계
- 기획 데이터는 구글 시트에 있고 코드가 값으로 연결, `players.sword_level` → `swords.level`, `player_artifacts.code` → `artifacts.code`이며, 아티팩트 능력치는 `artifact_stats`, 강화 비용은 `artifact_enhance`에서 검색
- 랭킹 조회용 사본은 Redis에 존재 ([ranking](docs/ranking.md)).
 
**핵심 설계**
 
- **중복·동시 요청에 안전한 처리:** 재화가 바뀌는 요청은 멱등성 키로 한 번만 처리하고, 재화·아티팩트는 낙관적 락(`Version`), 장착 슬롯은 유니크 제약으로 보호
- **재화 추적:** 지급·차감을 변동량과 잔액으로 남기고, 매일 잔액과 기록 합계를 비교해 불일치 여부 검사
- **기획 데이터 분리:** 강화 확률·비용·능력치를 범용 파싱 테이블을 통해 구글 시트에서 읽고, 서버 시작 시 검증해 잘못된 값이 있으면 기동을 정지
- **랭킹 정합성과 확장성:** 기록은 DB upsert 한 번으로 비교·저장하고 커밋 후에만 Redis에 반영, 조회는 Redis Sorted Set으로 처리해 인원이 늘어도 순위 계산이 빠름

  </div>
</details>

<details>
  <summary>전투 및 보상</summary>
  <div markdown="1">

  여기에 숨겨둘 텍스트, 이미지, 코드 등을 작성합니다.(첫줄을 남겨놓아야 줄바꿈이 가능함 <- 이유는 모름)

  </div>
</details>
