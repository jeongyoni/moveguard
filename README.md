# MoveGuard

IDC → 클라우드(AWS) **이전(마이그레이션) 사업의 전환 위험을 사전 진단**하는 도구입니다.
이전 대상 자산의 네트워크·DNS·인증서·소프트웨어 버전·백업 정보를 입력받아 규칙으로 위험을 판정하고, 위험도를 산정해 **전환 가능/차단** 여부를 알려줍니다.

## 주요 기능

- 이전사업 단위로 자산·IP·의존관계·DNS·인증서·설치 소프트웨어·백업 데이터를 진단 입력으로 수집
- 규칙 기반 위험 진단 (FMEA RPN 점수화) — 네트워크·DNS·보안·호환성·백업
- 위험도 등급 산정 및 **전환 차단(blocking)** 판정
- 규칙별 조치 가이드 문구 제공
- OS·WAS·DB 버전의 지원 종료(EOL)를 **endoflife.date와 동기화**해 진단
- 가상 이전사업 **시뮬레이션 → 성패 라벨링 → 규칙 성능 분석 → 학습**(규칙 baseline 비교)
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
| NETWORK_IP (공인IP·네트워크) | 0.350 | IP-01 공인IP 변경 · IP-02 내부 공인IP 사용 · IP-03 IP 하드코딩 · IP-04 허용목록 IP 변경 · IP-05 이전 후 IP 미확정 |
| COMPAT (OS·DBMS 호환성) | 0.200 | CMP-01 EOL 버전 · CMP-02 최소 Java 미달 · CMP-03 javax→jakarta · CMP-04 지원종료 임박 · CMP-05 메이저 건너뛰기 |
| DNS (DNS 전환) | 0.150 | DNS-01 긴 TTL · DNS-02 이전 후 레코드 누락 |
| BACKUP (백업·복구) | 0.150 | BAK-01 최근 백업 없음 · BAK-02 복구 테스트 미수행 · BAK-03 이전 후 백업 미비 · BAK-04 오프사이트 미보관 |
| SECURITY (보안·접근통제) | 0.150 | PORT-01 민감 포트 노출 · PORT-02 평문 프로토콜 · CERT-01 인증서 만료 임박 · CERT-02 갱신 계획 누락 |

## 아키텍처

```
입력 DB
  → DiagnosisContextLoader   자산·IP·의존·DNS·인증서·소프트웨어·호환성기준·전환예정일 로딩
  → DiagnosisContext         규칙이 참조하는 진단 입력 객체
  → DiagnosisEngine          규칙 적용 + RPN 정렬 + 점수 산정 (진단·시뮬레이션 공유)
     → RiskRuleEvaluator[]   @Component로 자동 수집되는 규칙별 판정기 → Finding
     → RiskScorer            요인 가중 총점, 등급/차단/최대 RPN
  → diagnosis_run / diagnosis_finding 저장
  → DiagnosisResult          API 응답
```

- 규칙은 `risk_rule` 테이블에 **활성화된 것만** 평가에 반영됩니다. 새 규칙은 `RiskRuleEvaluator` 구현체(@Component)와 `risk_rule` 시드가 모두 있어야 동작합니다.

### 패키지 구성

```
com.moveguard
├─ asset        # 진단 입력 도메인 (Asset, AssetIp, Dependency, DnsRecord, Certificate, AssetSoftware, BackupPlan, Phase)
├─ project      # 이전사업 조회 API
├─ compat       # 호환성 기준 데이터 + endoflife.date 동기화 (CompatSyncService)
├─ diagnosis    # 진단 파이프라인 (DiagnosisEngine)
│  └─ rule      # 규칙 구현체 (RiskRuleEvaluator)
└─ sim          # 시뮬레이션·학습 (ScenarioGenerator, OutcomeModel, RuleAnalysis)

ml/            # 성패 예측 학습 스크립트 (Python, scikit-learn)
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
docker exec -i moveguard-mysql mysql -uroot -proot1234 moveguard < src/main/resources/db/compat_schema.sql
docker exec -i moveguard-mysql mysql -uroot -proot1234 moveguard < src/main/resources/db/compat_data.sql
```

> 외래키 때문에 적용 순서를 지켜야 합니다: `schema.sql` → `data.sql` → `compat_schema.sql` → `compat_data.sql`

> **윈도우에서는 PowerShell이 아니라 Git Bash(또는 cmd)에서 실행하세요.** PowerShell의 파이프는 파일을
> 콘솔 코드페이지로 다시 인코딩해서 한글이 전부 `?`로 저장됩니다. 이미 깨졌다면 위 4개를 다시 적용하면 됩니다.

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

## 화면

앱을 띄우고 <http://localhost:8080/> 으로 접속하면 진단 결과를 사람이 보는 화면으로 확인할 수 있습니다.

| 경로 | 설명 |
| --- | --- |
| `GET /` | 이전사업 목록 — 사업별 **진단 실행** 버튼 |
| `POST /projects/{projectId}/diagnose` | 진단을 실행하고 결과 화면을 보여줌 |

결과 화면에 나오는 것

- **판정**: 위험 등급(HIGH/MEDIUM/LOW), 전환 차단 여부, 최고 RPN, 종합 점수, 발견 건수
- **진단 입력**(접이식): 무엇을 보고 판단했는지 — 자산별 IP·소프트웨어 버전의 **이전 전/후 비교**(바뀐 값 강조), 의존관계·DNS·인증서·백업
- **위험요인별**: 5개 요인(공인IP·네트워크 / OS·DBMS 호환성 / 보안·접근통제 / DNS / 백업·복구)의 건수와 최고 RPN
- **발견 목록**: RPN 내림차순으로 규칙 코드·제목·차단 배지와 함께 **무엇이 왜 위험한지(message)** 와 **조치 가이드(mitigation)**

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/projects` | 이전사업 목록 |
| GET | `/api/projects/{projectId}` | 이전사업 단건 |
| POST | `/api/projects/{projectId}/diagnoses` | 진단 실행 (결과 반환·저장) |
| POST | `/api/compat/sync` | 호환성 기준 데이터를 endoflife.date와 동기화 (실패 시 스냅샷 fallback) |
| GET | `/api/sim/dataset?count=&seed=` | 가상 이전사업 데이터셋(특징+진단결과+성패) CSV |
| GET | `/api/sim/analysis?count=&seed=` | 규칙(차단)의 성패 예측 성능 분석 (혼동행렬·정밀도·재현율·F1) |

진단 실행 예시:

```bash
curl -X POST http://localhost:8080/api/projects/1/diagnoses
```

응답(`DiagnosisResult`)에는 위험도 등급, 전환 차단 여부, 최대 RPN, 발견된 위험 항목(규칙 코드·문구·조치 가이드)이 포함됩니다.

## 시뮬레이션 · 학습

규칙 진단 엔진을 재사용해 가상 이전사업을 대량 생성하고, 학습 데이터를 만든다. 상세 계획은 [`docs/SIMULATION_ROADMAP.md`](docs/SIMULATION_ROADMAP.md).

- **생성·라벨링·분석**: `/api/sim/dataset`(데이터셋), `/api/sim/analysis`(규칙 성능). 모두 인메모리 평가로 DB에 저장하지 않음
- **학습**: `ml/`의 Python 스크립트로 성패를 예측하고 규칙 baseline과 F1 비교 (`ml/README.md` 참고)
- 표(tabular) 데이터라 **CPU로 충분** — GPU는 이후 딥러닝·대규모 단계에서만 필요

## 개발 규칙

- 브랜치·커밋·PR 규칙은 [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md) 참고
- 흐름: 이슈 → `feature/<번호>-<이름>` 브랜치 → 규칙별 커밋(각 커밋 테스트 통과) → PR(`Closes #`) → 리뷰·병합
- commit-msg 훅으로 커밋 메시지 규칙을 검증합니다. 클론 후 한 번 활성화하세요.

```bash
git config core.hooksPath .githooks
```

## 범위

- **1차**: 이전사업 입력 → 네트워크·DNS·보안·호환성·백업 진단 → 위험도 산정 _(완료)_
  - 위험요인 5개 전부 구현(NETWORK_IP·DNS·SECURITY·COMPAT·BACKUP), 규칙 20개
- **시뮬레이션·학습**: 가상 데이터 생성 → 성패 라벨 → 규칙 분석 → 학습 _(파이프라인 완료)_
- **2차**: 전환 실행 · 검증 · 롤백 _(예정)_
