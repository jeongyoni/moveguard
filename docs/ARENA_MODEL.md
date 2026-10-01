# 전환 과정 Arena 모델 + 7분 영상 가이드

발표 영상(약 7분)용. Rockwell Arena에서 **전환(cutover) 과정 이산사건 시뮬레이션**을 만들고 녹화하는 방법.
모델은 `des/cutover_sim.py`(SimPy 프로토타입)와 동일하게 맞춘다 → 프로젝트 전체가 하나로 이어진다.

> **환경**: Arena는 윈도우 전용. 맥이면 윈도우 PC(실습실) 또는 VM 필요.
> 무료로는 **Arena Student/Evaluation Edition**으로 충분(이 모델 규모 OK).

---

## 1. 모델 개요

자산(서버) 여러 대가 **한정된 엔지니어**를 나눠 쓰며 전환 단계를 거친다. 엔지니어가 부족하면 **대기열(병목)** 이 생긴다.

```
Create(자산 N개 생성) → Seize(엔지니어 1명)
   → Process 백업 → Process 서비스중단 → Process 데이터이전 → Process 재설정
   → Decide(검증 성공/실패?)
        ├─ 실패 → Process 롤백 → Record "실패" → Release → Dispose(FAIL)
        └─ 성공 →                Record "성공" → Release → Dispose(SUCCESS)
```

## 2. 모듈별 설정 (Basic Process 패널)

| 모듈 | 종류 | 핵심 설정 |
| --- | --- | --- |
| **자산 생성** | Create | Entities per Arrival = `6`(자산 수), Max Arrivals = `1`, First Creation = `0.0` → t=0에 6개 생성 |
| **엔지니어 확보** | Seize (또는 Process의 Seize) | Resource = `Engineer`, 수량 1 |
| **백업** | Process (Delay) | Delay = `TRIA(15,25,50)` 분 |
| **서비스중단** | Process (Delay) | `TRIA(3,5,10)` |
| **데이터이전** | Process (Delay) | `UNIF(20,200)/2` (용량 GB ÷ 2GB/분) |
| **재설정** | Process (Delay) | `TRIA(10,20,40) * vReconfigMult` |
| **검증** | Decide | 2-way by Chance, Percent True = `vFailPct`(%) → True 분기 = 실패 |
| **롤백** | Process (Delay) | `TRIA(30,45,90)` |
| **성공/실패 집계** | Record | Type = Count, 각각 "성공"/"실패" 카운터 |
| **엔지니어 반납** | Release | Resource = `Engineer` |
| **종료** | Dispose | 성공/실패 각각 1개 |

> 팁: 재설정까지 한 엔지니어가 계속 잡고 있어야 하므로 **맨 앞 Seize → 맨 뒤 Release**. 중간 단계는 Process의 Action을 **Delay**(자원 재점유 X)로.

## 3. 데이터·자원·실행 설정

**Resource** (Resource 데이터 모듈)
- `Engineer` : Capacity = `2` (시나리오에 따라 2/3/4로 변경)

**Variable** (Variable 데이터 모듈) — 위험 수준이 좌우
| 변수 | LOW | MEDIUM | HIGH |
| --- | --- | --- | --- |
| `vFailPct` (검증 실패 %) | 5 | 20 | 45 |
| `vReconfigMult` (재설정 지연 배수) | 1.0 | 1.3 | 1.7 |

**Run > Setup**
- Replication Length = `24` Hours (충분히 길게), Base Time Units = `Minutes`
- Number of Replications = `500`
- 정비창(Maintenance Window) = `6시간 = 360분` (판정 기준)

> 이 숫자들은 `des/cutover_sim.py`와 동일 — Arena 결과가 SimPy 결과(아래)와 비슷하게 나와야 정상.

## 4. 볼 것 (결과 · 애니메이션)

- **애니메이션**: 엔지니어 앞에 자산들이 **줄 서는 모습**(병목) — 영상의 핵심 볼거리
- **리포트(Reports)**: `Engineer` 가동률(Utilization), 성공/실패 Count, 엔티티 총 소요시간(Total Time)
- **시나리오 비교**: 엔지니어 2 → 4명, 위험 LOW → HIGH 로 바꿔 Run → 대기열·완료시간 변화

참고 — SimPy 프로토타입 결과(HIGH·자산6·정비창6h):

| 엔지니어 | 평균 소요 | 정비창 완료율 |
| --- | --- | --- |
| 2명 | 8.4h | 0.6% |
| 3명 | 6.0h | 51% |
| 4명 | 5.2h | 87% |

---

## 5. 7분 영상 스토리보드

| 시간 | 장면 | 내레이션 요지 |
| --- | --- | --- |
| **0:00–0:45** | 제목 + MoveGuard 한 줄 | "IDC→클라우드 이전 위험을 진단하는 MoveGuard. 여기선 전환 '과정'을 시뮬레이션해 **정비창 안에 끝나는지** 본다." |
| **0:45–2:30** | Arena 모델 전체 흐름 훑기 | 모듈 따라가며: 자산 생성 → 엔지니어 확보 → 백업·중단·이전·재설정 → 검증 → (실패 시 롤백) → 종료. 각 단계가 실제 전환 작업임을 설명 |
| **2:30–4:00** | 실행 ① 엔지니어 2명·HIGH | Run ▶ — **엔지니어 앞 대기열이 길게 쌓이는** 애니메이션. "위험 높고 인력 부족 → 지연·롤백 다수" |
| **4:00–5:30** | 실행 ② 엔지니어 4명(또는 LOW) | 설정 바꿔 Run ▶ — 대기열 짧아지고 빨리 끝남. 두 결과 나란히 비교 |
| **5:30–6:30** | 리포트 화면 | 가동률·성공/실패 수·평균 소요시간. 표로 "2명 0.6% → 4명 87% 정비창 완료" 강조 |
| **6:30–7:00** | 마무리 | "MoveGuard 진단의 위험 수준(HIGH/MED/LOW)이 이 시뮬의 실패확률·지연으로 연결된다 → **진단과 일정 계획이 하나로**." |

### 녹화 팁
- 화면 녹화: 윈도우 **Xbox Game Bar(Win+G)** 또는 OBS
- Run Speed Factor를 **빠르게** 해 애니메이션이 7분 안에 끝나게 조절
- 2:30/4:00 두 실행은 **Run을 미리 한 번 돌려두고** 녹화하면 매끄러움
- 숫자는 리포트 캡처로 보여주고, 말로 "정비창 완료율"을 해석

---

## 6. 체크리스트

- [ ] 윈도우 + Arena 설치 (Student/Eval)
- [ ] 위 모듈대로 모델 작성, Variable/Resource/Run 설정
- [ ] 엔지니어 2명·HIGH로 1차 Run → 애니메이션·리포트 확인
- [ ] 엔지니어 4명(또는 LOW)로 2차 Run → 비교
- [ ] 스토리보드대로 7분 녹화 → 발표 자료에 삽입
