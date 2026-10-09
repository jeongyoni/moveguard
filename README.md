# MoveGuard

**시스원 MSP 사업부 기술영업 도구** — 고객이 **운영 중인 환경**(IDC 리눅스 서버 등)을 "무료 헬스체크"로 점검해
리눅스 OS·DB 지원종료(EOL)·인증서·백업 리스크를 사고 전에 선제 발견하고, 그 리스크를 **MSP 운영·보안·백업·DR 수주로 연결**합니다.
운영 환경의 자산·소프트웨어 버전·인증서·백업 정보를 입력받아 규칙으로 위험을 판정하고, 위험도를 산정해 **즉시 조치/양호** 여부를 알려줍니다.

> 원래 "클라우드 이전 위험 진단"으로 시작했으나, MSP 본업(IDC·운영)에 맞춰 **운영 환경 헬스체크**로 재정의했습니다.
> 이전(migration) 전용 규칙(공인IP 변경·DNS 전환 등)은 비활성화되어 있고, 운영 3영역 13개 규칙이 활성입니다.

## 주요 기능

**진단 엔진**
- 운영 환경 단위로 자산·IP·의존관계·DNS·인증서·설치 소프트웨어·백업 데이터를 진단 입력으로 수집
- 규칙 기반 위험 진단 (FMEA RPN 점수화) — OS·DBMS 호환성 · 보안 · 백업 (운영 3영역 13개 규칙)
- 위험도 등급(HIGH/MEDIUM/LOW) 산정 및 **즉시 조치(blocking)** 판정
- 규칙별 "왜 위험한지 + MSP 서비스로 어떻게 해결하는지" 조치 가이드 제공
- 리눅스 OS·WAS·DB 버전의 지원 종료(EOL)를 **endoflife.date와 동기화**해 진단 (CentOS 7 등)

**화면 (어드민 콘솔 UI)**
- **대시보드 모니터링 콘솔** — 전체 환경의 위험 등급 분포·환경별 최고 RPN 차트 + 현황 테이블
- **진단 결과** — 통계 카드 + **AI 경영요약** + 발견 리스크(반복 규칙은 묶음) + 위험요인 차트
- **진단 이력·추이** — 실행별 RPN·발견 건수 추이 그래프 (조치 효과 시각화)
- **제안서용 리포트** — 통계·AI 요약·권고 조치가 담긴 인쇄/PDF 리포트
- **지원종료(EOL) 기준** — 제품·버전별 지원 상태 조회 + 동기화
- **AI 경영요약** — 진단 결과를 고객 보고용 문장으로 **규칙 기반 자동 생성**(외부 AI 미사용 · 데이터 미유출)

**입력**
- **엑셀 업로드**로 환경 등록 (양식 다운로드 → 검증 → 저장 → 진단)
- **웹 폼**으로 환경 생성 및 자산·IP·DNS·인증서·소프트웨어·백업 항목 추가/수정/삭제 + 화면마다 다시 진단
- **WAR/JAR 분석** — 배포 파일에서 하드코딩 IP·컴파일된 Java 버전·서블릿 스펙·라이브러리 자동 추출(메모리에서만 분석)

**이력·검증**
- 진단 이력(run·finding) 저장 및 **재진단 비교**(직전 대비 등급·건수·RPN 변화)
- 가상 운영 환경 **시뮬레이션 → 성패 라벨링 → 규칙 성능 분석 → 학습**(규칙 baseline 비교)

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 4.1.1 (Web MVC, Validation, Thymeleaf) |
| DB 접근 | MyBatis (mybatis-spring-boot-starter 4.1.0) |
| DB | MySQL 8.0 |
| 빌드 | Gradle |
| 프런트 | Thymeleaf 프래그먼트(공용 셸) · Chart.js(로컬 벤더링) |
| 분석/학습 | Python (scikit-learn), 전환 시뮬레이션 SimPy, 로컬 LLM 리포트 Ollama |
| 기타 | Lombok |

## 진단 모델 (FMEA 기반)

각 규칙은 다음으로 점수화됩니다.

```
RPN(위험우선순위수) = 심각도(severity) × 발생가능성(occurrence) × 검출난이도(detection)   // 최대 1000
```

- 규칙은 5개 **위험요인(factor)** 중 하나에 속하며, 요인마다 가중치가 있습니다.
- 규칙에 `is_blocking`이 켜져 있으면 점수와 무관하게 **즉시 조치 필요**로 판정됩니다.

### 위험요인과 규칙

운영 모드에서는 **운영 3영역(OS·DBMS 호환성 · 보안 · 백업)의 13개 규칙이 활성**이고, 이전(migration) 전용 규칙(공인IP·DNS·javax→jakarta·메이저 건너뛰기)은 비활성입니다. 아래는 전체 규칙 카탈로그입니다(⚙=운영 활성, ✈=이전 전용·비활성).

| 요인 | 가중치 | 규칙 |
| --- | --- | --- |
| COMPAT (OS·DBMS 호환성) | 0.200 | ⚙CMP-01 OS/DB EOL · ⚙CMP-02 최소 Java 미달 · ✈CMP-03 javax→jakarta · ⚙CMP-04 지원종료 임박 · ✈CMP-05 메이저 건너뛰기 · ⚙CMP-06 연장지원(유상) 구간 · ⚙CMP-07 JDBC 드라이버 버전 미달 |
| SECURITY (보안·접근통제) | 0.150 | ⚙PORT-01 민감 포트 노출 · ⚙PORT-02 평문 프로토콜 · ⚙CERT-01 인증서 만료 임박 · ⚙CERT-02 갱신 계획 누락 |
| BACKUP (백업·복구) | 0.150 | ⚙BAK-01 최근 백업 없음 · ⚙BAK-02 복구 테스트 미수행 · ⚙BAK-03 백업 체계 미비 · ⚙BAK-04 오프사이트 미보관 |
| NETWORK_IP (공인IP·네트워크) | 0.350 | ✈IP-01 공인IP 변경 · ✈IP-02 내부 공인IP 사용 · ✈IP-03 IP 하드코딩 · ✈IP-04 허용목록 IP 변경 · ✈IP-05 전환 후 IP 미확정 |
| DNS (DNS 전환) | 0.150 | ✈DNS-01 긴 TTL · ✈DNS-02 전환 후 레코드 누락 |

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
├─ project      # 운영 환경(사업) 조회 API
├─ compat       # 호환성 기준 데이터 + endoflife.date 동기화 (CompatSyncService)
├─ diagnosis    # 진단 파이프라인·화면 (DiagnosisEngine, DashboardService, ExecutiveSummary, DiagnosisViewController)
│  └─ rule      # 규칙 구현체 (RiskRuleEvaluator)
└─ sim          # 시뮬레이션·학습 (ScenarioGenerator, OutcomeModel, RuleAnalysis)

ml/            # 성패 예측 학습 스크립트 (Python, scikit-learn)
report/        # 로컬 LLM(Ollama) 리포트 생성 스크립트 (옵션)
des/           # 전환 과정 이산사건 시뮬레이션 (SimPy)
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

### 3. 로컬 설정 (`application-local.yml`)

DB 접속 정보가 든 `src/main/resources/application-local.yml`은 비밀정보라 저장소에 포함하지 않습니다(.gitignore).
아래 내용으로 직접 만드세요. (docker-compose 기본값 기준)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moveguard?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: root1234
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080`, 프로필은 `local`(→ `application-local.yml`)입니다.

### 5. 운영 환경 등록 (두 가지 방법)

브라우저 `http://localhost:8080/` 에서 시드 없이 환경을 넣을 수 있습니다.

**방법 A — 엑셀 업로드 (한 번에 통째로)**
- **"엑셀로 등록"** → 양식 다운로드(.xlsx) → 8개 시트(환경·자산·IP·의존관계·DNS·인증서·소프트웨어·백업) 작성 → 업로드
- 전건 검증 → 하나라도 틀리면 `"자산 시트 3행: ..."`처럼 시트·행·이유를 표시하고 **저장 안 함**
- 통과하면 저장 후 **바로 진단 결과** 화면

**방법 B — 웹 폼 (항목별로 관리)**
- **"새 환경 등록"** → 기본 정보 입력 → 생성되면 **환경 상세** 화면으로 이동
- 상세 화면에서 **자산 · IP · DNS · 인증서 · 소프트웨어 · 백업**을 각각 **추가 / 수정 / 삭제**
  - 자산을 먼저 등록해야 그 자산의 IP·소프트웨어·백업을 추가할 수 있음
  - 각 입력은 즉시 검증(형식·중복 등), 실패 시 저장하지 않고 사유 표시
- 화면의 **"다시 진단"** 으로 현재 입력 기준 진단을 다시 실행
- 대시보드(환경 목록)에서 **환경명을 클릭**하면 언제든 상세로 돌아와 편집

> 저장·검증 로직은 두 방법이 공유합니다(`ProjectImportService`·`ImportValues`).

### 6. 테스트

```bash
./gradlew test
```

> 통합 테스트(`DiagnosisServiceIntegrationTest`)는 로컬 MySQL에 더미 환경(`project_id = 1`) 시드가 필요합니다.

## 화면

앱을 띄우고 <http://localhost:8080/> 으로 접속하면 진단 결과를 사람이 보는 화면으로 확인할 수 있습니다.

| 경로 | 설명 |
| --- | --- |
| `GET /` | **대시보드** — 전체 환경 위험 현황·차트·현황 테이블 |
| `POST /projects/{projectId}/diagnose` | 진단 실행 → 결과 화면(통계·AI 요약·발견 리스크) |
| `GET /projects/{projectId}` | **환경 상세** — 자산·IP·DNS·인증서·소프트웨어·백업 CRUD |
| `GET /projects/{projectId}/history` | **진단 이력·추이** — RPN·건수 추이 그래프 |
| `GET /projects/{projectId}/report` | **제안서용 리포트** (인쇄/PDF) |
| `GET /projects/{projectId}/war-scan` | **WAR/JAR 분석** 업로드·결과 |
| `GET /eol` | **지원종료(EOL) 기준** 조회·동기화 |
| `GET /projects/new`, `GET /projects/import` | 웹 폼 / 엑셀 업로드 등록 |

진단 결과 화면에 나오는 것

- **통계 카드**: 위험 등급(HIGH/MEDIUM/LOW), 최고 RPN, 발견 건수, 긴급 건수
- **AI 경영요약**: 등급·최상위 리스크·영역별 요약·권고 MSP 서비스 (규칙 기반 자동 생성)
- **발견 목록**: RPN 내림차순, **같은 규칙이 여러 자산에서 걸리면 한 카드로 묶음**(예: EOL), 규칙마다 message·mitigation
- **위험요인 차트** + **재진단 비교**(직전 대비 등급·건수·RPN)
- **진단 입력**(접이식): 무엇을 보고 판단했는지 — 자산별 IP·소프트웨어 버전의 기준/현재 비교, 인증서·백업

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/projects` | 운영 환경(사업) 목록 |
| GET | `/api/projects/{projectId}` | 운영 환경(사업) 단건 |
| POST | `/api/projects/{projectId}/diagnoses` | 진단 실행 (결과 반환·저장) |
| POST | `/api/compat/sync` | 호환성 기준 데이터를 endoflife.date와 동기화 (실패 시 스냅샷 fallback) |
| GET | `/api/sim/dataset?count=&seed=` | 가상 운영 환경 데이터셋(특징+진단결과+성패) CSV |
| GET | `/api/sim/analysis?count=&seed=` | 규칙(차단)의 성패 예측 성능 분석 (혼동행렬·정밀도·재현율·F1) |

진단 실행 예시:

```bash
curl -X POST http://localhost:8080/api/projects/1/diagnoses
```

응답(`DiagnosisResult`)에는 위험도 등급, 즉시 조치 여부, 최대 RPN, 발견된 위험 항목(규칙 코드·문구·조치 가이드)이 포함됩니다.

## 시뮬레이션 · 학습

규칙 진단 엔진을 재사용해 가상 운영 환경을 대량 생성하고, 학습 데이터를 만든다. 상세 계획은 [`docs/SIMULATION_ROADMAP.md`](docs/SIMULATION_ROADMAP.md).

- **생성·라벨링·분석**: `/api/sim/dataset`(데이터셋), `/api/sim/analysis`(규칙 성능). 모두 인메모리 평가로 DB에 저장하지 않음
- **학습**: `ml/`의 Python 스크립트로 성패를 예측하고 규칙 baseline과 F1 비교 (`ml/README.md` 참고)
- 표(tabular) 데이터라 **CPU로 충분** — GPU는 이후 딥러닝·대규모 단계에서만 필요

## AI 경영요약 · 리포트

- **앱 내 AI 경영요약(기본)**: 진단 결과를 고객 보고용 문장으로 **규칙 기반 자동 생성**한다(`ExecutiveSummary`).
  외부 AI를 호출하지 않아 고객 데이터가 회사 밖으로 나가지 않으며, 진단 결과·제안서 리포트 화면에 바로 표시된다.
- **로컬 LLM 리포트(옵션·오프라인)**: 동일한 진단 결과(JSON)를 **로컬 LLM**(Ollama)로 더 긴 보고서 문장으로 작성하는 별도 스크립트.
  외부 API 없이 Ollama HTTP API만 호출한다. 설치·실행은 [`report/README.md`](report/README.md). *(앱 통합은 로드맵)*

```bash
cd report
python generate_report.py --dry-run                                   # 프롬프트만 확인(LLM 불필요)
python generate_report.py --model qwen2.5:14b --out report.md         # 로컬 LLM에서 실제 생성
```

## 개발 규칙

- 브랜치·커밋·PR 규칙은 [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md) 참고
- 흐름: 이슈 → `feature/<번호>-<이름>` 브랜치 → 규칙별 커밋(각 커밋 테스트 통과) → PR(`Closes #`) → 리뷰·병합
- commit-msg 훅으로 커밋 메시지 규칙을 검증합니다. 클론 후 한 번 활성화하세요.

```bash
git config core.hooksPath .githooks
```

## 범위

- **진단 엔진** _(완료)_ — 위험요인 5개·규칙 22개 구현, 기본 활성은 **운영 헬스체크 3영역**(COMPAT·SECURITY·BACKUP, 규칙 13개). 전환 전용(NETWORK_IP·DNS·CMP-03/05)은 enabled=0 비활성
- **웹 UI** _(완료)_ — 대시보드 모니터링 콘솔 · 진단 결과(AI 요약·반복 규칙 묶음·차트) · 진단 이력/추이 · 제안서 리포트(PDF) · EOL 기준 조회 · 환경 상세 CRUD · 엑셀 업로드 · WAR/JAR 분석
- **입력 간소화** _(완료)_ — 엑셀 업로드 + 웹 폼 + 배포 파일(WAR) 자동 추출
- **시뮬레이션·학습** _(파이프라인 완료)_ — 가상 운영 환경 생성 → 성패 라벨 → 규칙 분석 → ML 학습(규칙 baseline 비교, 시뮬레이션 기준 F1 측정)
- **향후** — 실제 운영 데이터로 ML 재학습 · 사내 LLM 리포트 앱 통합 · 전환 실행/검증/롤백 관리
