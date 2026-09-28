# MoveGuard

IDC → 클라우드(AWS) **이전(마이그레이션) 사업의 전환 위험을 사전 진단**하는 도구입니다.
이전 대상 자산의 네트워크·DNS·인증서 정보를 입력받아 규칙으로 위험을 판정하고, 위험도를 산정해 **전환 가능/차단** 여부를 알려줍니다.

## 주요 기능

- 이전사업 단위로 자산·IP·의존관계·DNS·인증서 데이터를 진단 입력으로 수집
- 규칙 기반 위험 진단 (FMEA RPN 점수화)
- 위험도 등급 산정 및 **전환 차단(blocking)** 판정
- 규칙별 조치 가이드 문구 제공
- 진단 이력(run·finding) 저장

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 4.1.1 (Web MVC, Validation, Thymeleaf) |
| DB 접근 | MyBatis (mybatis-spring-boot-starter 4.1.0) |
| DB | MySQL 8.0 |
| 빌드 | Gradle |
| 기타 | Lombok |

## 진단 모델 (FMEA 기반)

각 규칙은 다음으로 점수화됩니다.

```
RPN(위험우선순위수) = 심각도(severity) × 발생가능성(occurrence) × 검출난이도(detection)   // 최대 1000
```

- 규칙은 5개 **위험요인(factor)** 중 하나에 속하며, 요인마다 가중치가 있습니다.
- 규칙에 `is_blocking`이 켜져 있으면 점수와 무관하게 **전환이 차단**됩니다.

### 위험요인과 규칙

| 요인 | 가중치 | 규칙 |
| --- | --- | --- |
| NETWORK_IP (공인IP·네트워크) | 0.350 | IP-01 공인IP 변경 · IP-02 내부 공인IP 사용 · IP-03 IP 하드코딩 · IP-04 허용목록 IP 변경 |
| COMPAT (OS·DBMS 호환성) | 0.200 | _(예정)_ |
| DNS (DNS 전환) | 0.150 | DNS-01 긴 TTL · DNS-02 이전 후 레코드 누락 |
| BACKUP (백업·복구) | 0.150 | _(예정)_ |
| SECURITY (보안·접근통제) | 0.150 | PORT-01 민감 포트 노출 · PORT-02 평문 프로토콜 · CERT-01 인증서 만료 임박 · CERT-02 갱신 계획 누락 |

## 아키텍처

```
입력 DB
  → DiagnosisContextLoader   자산·IP·의존·DNS·인증서·전환예정일 로딩
  → DiagnosisContext         규칙이 참조하는 진단 입력 객체
  → RiskRuleEvaluator[]      @Component로 자동 수집되는 규칙별 판정기
  → Finding                  판정 결과 + 문구 치환 파라미터
  → RiskScorer               RPN 정렬, 요인 가중 총점, 등급/차단/최대 RPN
  → diagnosis_run / diagnosis_finding 저장
  → DiagnosisResult          API 응답
```

- 규칙은 `risk_rule` 테이블에 **활성화된 것만** 평가에 반영됩니다. 새 규칙은 `RiskRuleEvaluator` 구현체(@Component)와 `risk_rule` 시드가 모두 있어야 동작합니다.

### 패키지 구성

```
com.moveguard
├─ asset        # 진단 입력 도메인 (Asset, AssetIp, Dependency, DnsRecord, Certificate, Phase)
├─ project      # 이전사업 조회 API
└─ diagnosis    # 진단 파이프라인
   └─ rule      # 규칙 구현체 (RiskRuleEvaluator)
```

## 실행 방법

### 1. MySQL 기동 (docker-compose)

```bash
docker compose up -d
```

### 2. 스키마·시드 적용

`spring.sql.init.mode: never` 이므로 스키마와 시드는 수동으로 적용합니다.

```bash
docker exec -i moveguard-mysql mysql -uroot -proot1234 moveguard < src/main/resources/db/schema.sql
docker exec -i moveguard-mysql mysql -uroot -proot1234 moveguard < src/main/resources/db/data.sql
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080`, 프로필은 `local`(→ `application-local.yml`)입니다.

### 4. 테스트

```bash
./gradlew test
```

> 통합 테스트(`DiagnosisServiceIntegrationTest`)는 로컬 MySQL에 더미 사업(`project_id = 1`) 시드가 필요합니다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/projects` | 이전사업 목록 |
| GET | `/api/projects/{projectId}` | 이전사업 단건 |
| POST | `/api/projects/{projectId}/diagnoses` | 진단 실행 (결과 반환·저장) |

진단 실행 예시:

```bash
curl -X POST http://localhost:8080/api/projects/1/diagnoses
```

응답(`DiagnosisResult`)에는 위험도 등급, 전환 차단 여부, 최대 RPN, 발견된 위험 항목(규칙 코드·문구·조치 가이드)이 포함됩니다.

## 개발 규칙

- 브랜치·커밋·PR 규칙은 [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md) 참고
- 흐름: 이슈 → `feature/<번호>-<이름>` 브랜치 → 규칙별 커밋(각 커밋 테스트 통과) → PR(`Closes #`) → 리뷰·병합
- commit-msg 훅으로 커밋 메시지 규칙을 검증합니다. 클론 후 한 번 활성화하세요.

```bash
git config core.hooksPath .githooks
```

## 범위

- **1차**: 이전사업 입력 → 공인IP·DNS 진단 → 위험도 산정 _(진행 중)_
- **2차**: 전환 실행 · 검증 · 롤백 _(예정)_
