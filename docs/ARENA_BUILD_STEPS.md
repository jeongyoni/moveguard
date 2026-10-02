# Arena 모델 작성 — 클릭 단위 순서

[`ARENA_MODEL.md`](ARENA_MODEL.md)의 모델을 **Arena를 처음 켠 상태에서 그대로 따라 만드는** 절차.
설정값의 근거와 영상 스토리보드는 `ARENA_MODEL.md`, 수치 비교 대상은 `../des/cutover_sim.py`.

> **식별자는 영문으로 쓴다.** 모듈·자원·변수 이름은 `NC(Cnt_Success)` 처럼 수식에서 다시 참조되는데,
> Arena는 한글 식별자에서 종종 깨진다. 화면에 보여줄 한글은 **Draw ▸ Text(A)** 로 따로 올린다.

---

## 0. 설치 — 다운로드 자격 얻기까지 (먼저 시작할 것)

**Arena Student Edition 은 바로 못 받는다.** Rockwell 계정에 학교를 등록하고 **승인까지 약 2일**이 걸리니,
모델을 만들 날짜보다 며칠 앞서 신청해둬야 한다.

1. Rockwell 계정 생성 후 **로그인**
2. **프로필을 끝까지 완성** (미완성이면 다음 단계가 막힘)
3. **Access ▸ Locations** 에서 **학교를 검색해 등록**
4. 등록 심사 대기 — **약 2일**. 승인되면 Arena Student / Evaluation Edition 다운로드가 열린다
5. 설치 (winget에는 없음 — 받은 설치 파일로 진행)

> 신청 기록: 2026-10-02 학교 등록 신청 → 승인 대기 중.
> 승인을 기다리는 동안은 `des/cutover_anim.py`(Salabim)로 같은 모델의 애니메이션을 윈도우에서 돌려볼 수 있다.

**승인·설치가 끝났으면**

6. Arena 실행 → `File ▸ New` (Ctrl+N)
7. `File ▸ Save As` → 리포지터리의 **`arena/cutover.doe`** 로 저장 (폴더는 새로 만들면 됨)

왼쪽 **Project Bar** 에 `Basic Process` / `Advanced Process` / `Reports` / `Navigate` 패널이 보이면 준비 완료.
위쪽은 플로우차트 캔버스, 아래쪽은 **데이터 모듈 스프레드시트**(패널에서 데이터 모듈을 클릭하면 표시).

---

## 1. 모듈 배치

`Object ▸ Auto-Connect` 를 **켜두면** 드래그 순서대로 자동 연결된다 (연결선을 직접 그릴 땐 `Object ▸ Connect`).

**Basic Process** 패널에서 순서대로 캔버스에 드래그:

| # | 모듈 | 이름(Name) |
| --- | --- | --- |
| 1 | Create | `CreateAssets` |
| 2 | *(Advanced Process 패널)* Seize | `SeizeEngineer` |
| 3 | Process | `Backup` |
| 4 | Process | `ServiceStop` |
| 5 | Process | `DataMove` |
| 6 | Process | `Reconfigure` |
| 7 | Decide | `Validate` |
| 8 | Process | `Rollback` |
| 9 | Record | `RecFail` |
| 10 | Record | `RecSuccess` |
| 11 | Release | `ReleaseFail` |
| 12 | Release | `ReleaseSuccess` |
| 13 | Dispose | `DisposeFail` |
| 14 | Dispose | `DisposeSuccess` |

연결 모양:

```
CreateAssets → SeizeEngineer → Backup → ServiceStop → DataMove → Reconfigure → Validate
   Validate(True  = 실패) → Rollback → RecFail    → ReleaseFail    → DisposeFail
   Validate(False = 성공) →            RecSuccess → ReleaseSuccess → DisposeSuccess
```

> Decide의 **True 분기가 "실패"** 다 (Percent True = 실패 확률). 헷갈리면 캔버스에 Text로 적어두자.

---

## 2. 모듈별 입력값

모듈을 **더블클릭**하면 대화상자가 열린다. 아래 값만 바꾸고 나머지는 기본값 유지.

### CreateAssets (Create)
- Entity Type: `Asset`
- Time Between Arrivals → Type: `Constant`, Value: `1`, Units: `Minutes` *(Max Arrivals=1이라 무의미)*
- **Entities per Arrival: `6`**
- **Max Arrivals: `1`**
- **First Creation: `0.0`**

→ t=0에 자산 6개가 한꺼번에 생성된다.

### SeizeEngineer (Seize)
- Resources 표에서 `Add...` → Type: `Resource`, Resource Name: `Engineer`, Quantity: `1`
- Queue Type: `Queue` (이름은 `SeizeEngineer.Queue` 자동 생성 — 애니메이션에서 줄 서는 그 대기열)

### Backup / ServiceStop / DataMove / Reconfigure (Process)
네 개 모두 **Type: `Standard`, Action: `Delay`** (자원을 다시 잡지 않음 — 이미 1번에서 확보)

| 모듈 | Delay Type | 입력 |
| --- | --- | --- |
| `Backup` | `Triangular` | Minimum `15` / Value `25` / Maximum `50`, Units `Minutes` |
| `ServiceStop` | `Triangular` | Minimum `3` / Value `5` / Maximum `10`, Units `Minutes` |
| `DataMove` | `Expression` | Expression: `UNIF(20,200)/2`, Units `Minutes` |
| `Reconfigure` | `Expression` | Expression: `TRIA(10,20,40) * vReconfigMult`, Units `Minutes` |

> `DataMove` 는 "용량 20~200GB ÷ 2GB/분". `Reconfigure` 는 위험 수준에 따라 늘어나는 단계.

### Validate (Decide)
- Type: `2-way by Chance`
- **Percent True (0-100): `vFailPct`** (숫자 대신 변수명을 그대로 입력)

### Rollback (Process)
- Action: `Delay`, Delay Type: `Triangular`, Minimum `30` / Value `45` / Maximum `90`, Units `Minutes`

### RecFail / RecSuccess (Record)
- Type: `Count`, Value: `1`
- Counter Name: `Cnt_Fail` / `Cnt_Success` ← **이 이름을 6절 종료조건에서 다시 쓴다**

### ReleaseFail / ReleaseSuccess (Release)
- Resources → `Add...` → Resource Name: `Engineer`, Quantity: `1`

### DisposeFail / DisposeSuccess (Dispose)
- `Record Entity Statistics` 체크 (총 소요시간 통계를 받기 위해)

---

## 3. Resource — 엔지니어

`Basic Process` 패널의 **Resource** 데이터 모듈 클릭 → 아래 스프레드시트에서 빈 행 더블클릭:

| Name | Type | Capacity | Report Statistics |
| --- | --- | --- | --- |
| `Engineer` | Fixed Capacity | `2` | ✔ |

Capacity가 시나리오 변수다 (2 → 3 → 4).

## 4. Variable — 위험 수준

`Basic Process` 패널의 **Variable** 데이터 모듈 → 두 행 추가. 각 행의 **Initial Values** 칸(`0 rows`)을 더블클릭해 값을 넣는다.

| Name | Initial Value (HIGH 기준) |
| --- | --- |
| `vFailPct` | `45` |
| `vReconfigMult` | `1.7` |

| 위험 수준 | `vFailPct` | `vReconfigMult` |
| --- | --- | --- |
| LOW | 5 | 1.0 |
| MEDIUM | 20 | 1.3 |
| HIGH | 45 | 1.7 |

## 5. Statistic — 정비창 완료율·소요시간

`Advanced Process` 패널의 **Statistic** 데이터 모듈 → 두 행 추가:

| Name | Type | Expression | Report Label |
| --- | --- | --- | --- |
| `Makespan_Min` | `Output` | `TNOW` | `Makespan (min)` |
| `WithinWindow` | `Output` | `TNOW <= 360` | `Within 6h window` |

- Output 통계는 **각 replication 끝에서 한 번** 평가된다. 6절의 종료조건 덕분에 그 시점의 `TNOW`가 곧 **마지막 자산 완료 시각(makespan)** 이다.
- `TNOW <= 360` 은 0/1 → 500회 평균이 그대로 **정비창 완료율**. `cutover_sim.py` 의 `makespan <= window` 와 같은 정의다.

## 6. Run ▸ Setup

**Replication Parameters 탭**

| 항목 | 값 |
| --- | --- |
| Number of Replications | `500` |
| Warm-up Period | `0` |
| Replication Length | `24` / Time Units `Hours` *(안전 상한)* |
| Hours Per Day | `24` |
| **Base Time Units** | `Minutes` |
| **Terminating Condition** | `NC(Cnt_Success) + NC(Cnt_Fail) >= 6` |

종료조건이 핵심이다 — 자산 6개가 모두 끝나면 replication을 끝내서 `TNOW`를 makespan으로 만든다.

**Project Parameters 탭**: Statistics Collection 에서 `Entities` · `Queues` · `Resources` · `Processes` 체크.

---

## 7. 애니메이션 꾸미기 (영상용)

1. **엔지니어 그림**: `Animate` 툴바 ▸ `Resource` → Identifier: `Engineer` → Idle/Busy 그림을 라이브러리(`People.plb` 등)에서 선택
2. **대기열**: `SeizeEngineer.Queue` 는 자동 생성 — 자산이 **줄 서는 모습**이 영상의 핵심이니 눈에 띄는 곳으로 끌어다 놓고 길이를 늘인다
3. **엔티티 그림**: `Basic Process ▸ Entity` 데이터 모듈 → `Asset` 행의 Initial Picture 지정
4. **실시간 카운터**: `Animate` 툴바 ▸ `Variable` → Expression `NC(Cnt_Success)` / `NC(Cnt_Fail)` 두 개 배치 → 성공·실패가 올라가는 게 화면에 보인다
5. **한글 라벨**: `Draw` 툴바의 `Text(A)` 로 "백업 / 서비스중단 / 데이터이전 / 재설정 / 검증 / 롤백" 을 각 모듈 위에 올린다

> 애니메이션을 볼 때는 Number of Replications 를 **1** 로 내리고, 숫자를 뽑을 때만 500으로 올린다.
> 속도는 `Run ▸ Speed ▸ Animation Speed Factor`(버전에 따라 `Run Control` 하위)로 조절.

---

## 8. 실행 — 두 시나리오

**① 엔지니어 2명 · HIGH** (대기열이 길게 쌓이는 장면)
- `Engineer` Capacity = `2`, `vFailPct` = `45`, `vReconfigMult` = `1.7`
- Replications = 1 → `Run ▸ Go` (F5) 로 애니메이션 녹화
- Replications = 500 + `Run ▸ Run Control ▸ Batch Run (No Animation)` 체크 → 숫자 뽑기

**② 엔지니어 4명 (또는 LOW)** (빨리 끝나는 장면)
- `Engineer` Capacity = `4` 로만 바꿔 같은 방식으로 1회 + 500회

시나리오를 바꿀 때 **건드리는 칸은 3개뿐**이다: Resource의 Capacity, Variable 두 개의 Initial Value.

## 9. 리포트에서 볼 숫자

실행이 끝나면 리포트 창(또는 `Reports` 패널 ▸ `Category Overview`):

| 보고 싶은 것 | 리포트 위치 |
| --- | --- |
| 정비창 완료율 | User Specified ▸ Output ▸ `Within 6h window` 평균 |
| 평균 소요시간(분) | User Specified ▸ Output ▸ `Makespan (min)` 평균 → ÷60 = 시간 |
| 성공/실패 건수 | User Specified ▸ Counter ▸ `Cnt_Success` / `Cnt_Fail` |
| 엔지니어 가동률 | Resource ▸ `Engineer` ▸ Scheduled/Instantaneous Utilization |
| 대기열 | Queue ▸ `SeizeEngineer.Queue` ▸ Waiting Time, Number Waiting |

**SimPy 프로토타입과 맞춰보기** (자산 6 · 정비창 6h · HIGH):

| 엔지니어 | 평균 소요 | 정비창 완료율 |
| --- | --- | --- |
| 2명 | 8.4h | 0.6% |
| 3명 | 6.0h | 51% |
| 4명 | 5.2h | 87% |

같은 분포·같은 규칙이라 **이 정도 범위로 나오면 모델이 맞게 들어간 것**이다. 많이 다르면 아래를 보자.

---

## 10. 어긋날 때 확인할 것

| 증상 | 원인 |
| --- | --- |
| `Unknown identifier: vFailPct` | Variable 데이터 모듈에 행이 없거나 이름 오타 |
| replication이 24시간을 꽉 채움 | Terminating Condition 의 Counter 이름이 Record 모듈의 Counter Name 과 불일치 |
| makespan이 터무니없이 작다 | 종료조건의 `>= 6` 이 Create의 Entities per Arrival(6)과 다름 |
| 자산이 Seize에서 안 빠져나옴 | `Engineer` Capacity가 0, 또는 Release 모듈 누락 |
| 소요시간이 너무 짧다 | Base Time Units 가 `Minutes` 가 아님(Hours면 60배) |
| 실패율이 거꾸로 | Decide의 **True = 실패**. 분기 연결이 뒤집혔는지 확인 |
| 모듈 추가가 막힘 | Student Edition 모델 크기 제한 — 이 모델(14개)은 여유 있음 |
| 수식에서 이름을 못 찾음 | 식별자에 한글 사용. 영문으로 바꾼다 |

## 11. 체크리스트

- [ ] Arena 설치 · `arena/cutover.doe` 로 저장
- [ ] 모듈 14개 배치 + 연결 (1절)
- [ ] 모듈별 값 입력 (2절)
- [ ] Resource `Engineer` = 2 (3절)
- [ ] Variable `vFailPct`=45, `vReconfigMult`=1.7 (4절)
- [ ] Statistic Output 2개 (5절)
- [ ] Run Setup — Base Time Units `Minutes`, Terminating Condition (6절)
- [ ] 애니메이션 꾸미기 (7절)
- [ ] 2명·HIGH / 4명 두 번 실행 (8절)
- [ ] SimPy 표와 숫자 비교 (9절)
- [ ] `ARENA_MODEL.md` 5절 스토리보드대로 7분 녹화
