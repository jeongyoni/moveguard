# 전환 과정 이산사건 시뮬레이션 (SimPy)

전환(cutover)을 이산사건 시뮬레이션으로 모델링해 **소요시간·정비창 완료율·롤백률·엔지니어 병목**을 추정한다.

Rockwell Arena와 같은 종류의 이산사건 시뮬레이션이지만, **맥에서 무료로 동작**하는 SimPy를 사용한다.

## 모델

자산(서버) N개가 한정된 엔지니어 E명을 공유하며 아래 단계를 거친다.

```
[엔지니어 확보 대기] → 백업 → 서비스중단 → 데이터이전(용량 의존)
   → 재설정(IP/DNS/인증서) → 검증(성공/실패) → 실패 시 롤백
```

- 각 단계 소요시간은 삼각분포로 샘플링
- **위험 수준(LOW/MEDIUM/HIGH)** 이 검증 실패확률과 재설정 지연을 좌우
- 이 위험 수준은 **MoveGuard 진단의 `riskLevel`** 과 연결할 수 있다(`--fetch`)
- 여러 번(replication) 반복해 분포를 추정

## 준비

**맥·리눅스**
```bash
cd des
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

**윈도우** (PowerShell) — tkinter가 기본 포함이라 추가 설치가 없다
```powershell
cd des
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 실행

```bash
# 위험 수준을 직접 지정
python cutover_sim.py --assets 6 --engineers 2 --window-hours 6 --risk HIGH --reps 500

# MoveGuard 진단 riskLevel을 위험 수준으로 사용 (앱 실행 중이어야 함)
python cutover_sim.py --fetch --project 1 --assets 6 --engineers 2
```

주요 옵션: `--assets`(자산 수) `--engineers`(엔지니어 수) `--window-hours`(정비창) `--risk`(LOW/MEDIUM/HIGH) `--reps`(반복 횟수) `--fetch`(진단 연동).

## 애니메이션 (발표 영상용) — `cutover_anim.py`

자산이 엔지니어를 기다리며 **줄 서고**, 각 전환 단계를 거쳐 성공/롤백으로 쌓이는 과정을 보여준다.

화면 구성
- **대기 / 작업 중 / 완료** 세 칸. 자산 블록에 **현재 단계**(백업·서비스중단·데이터이전·재설정·검증·롤백)가 색과 함께 표시된다
- **정비창 진행 바** — 넘어가면 빨강으로 바뀌고 `정비창 초과 +N.Nh` 가 뜬다
- 경과 시간, 성공·롤백 수, 엔지니어 가동 현황, 그리고 지금 무슨 일이 벌어지는지 한 줄 설명(병목 여부)

**준비 — tkinter 필요**
- **윈도우**: python.org 설치본에 tkinter가 **기본 포함** — 따로 할 것 없음
- **맥**: 별도 설치 (한 번만)
  ```bash
  brew install python-tk@3.14     # (python 버전에 맞게)
  ```

### 1) mp4로 바로 뽑기 (권장 — 화면 녹화 불필요)

```bash
python cutover_anim.py --engineers 2 --risk HIGH --speed 6 --video cutover_2eng_HIGH.mp4
python cutover_anim.py --engineers 4 --risk LOW  --speed 6 --video cutover_4eng_LOW.mp4
```

- `--video`를 주면 **창을 띄우지 않고**(salabim blind animation) 1280×720 / 30fps mp4를 쓴다. 창이 중간에 닫혀 녹화가 끊기는 일이 없다
- **두 시나리오는 같은 `--speed`로 뽑는다.** 영상 길이 차이(약 84초 vs 31초) 자체가 "인력을 늘리면 빨리 끝난다"는 메시지다
- `--seed`가 고정(기본 42)이라 같은 영상이 그대로 다시 나온다

### 2) 창으로 보며 직접 녹화

```bash
python cutover_anim.py --engineers 2 --risk HIGH   # 위험·인력 부족 → 대기열 길어짐
python cutover_anim.py --engineers 4 --risk LOW    # 비교: 빠르게 끝남
```
- **윈도우**: `Win + G`(Xbox Game Bar) 또는 OBS로 창 녹화
- **맥**: 화면 녹화 `⌘ + ⇧ + 5` 로 창 녹화

### 기타 옵션

| 옵션 | 설명 |
| --- | --- |
| `--speed` | 실시간 1초당 시뮬레이션 분 (기본 8). 영상 길이 = 총 소요분 ÷ speed |
| `--snapshots 60,180,360` | 해당 시각(분)의 화면을 png로 저장 — 발표 자료용 정지 이미지 |
| `--seed` | 난수 고정 (기본 42) |
| `--font` | 한글 글꼴. 기본값 윈도우 `malgun`, 맥 `AppleGothic` |
| `--headless` | 애니메이션 없이 숫자만 (검증용) |

영상 대본은 [`../docs/ARENA_MODEL.md`](../docs/ARENA_MODEL.md)의 "7분 영상 스토리보드" 참고(도구만 Salabim으로 바뀜).

## 활용 예

- **정비창 안에 끝나는가?** 위험 수준·엔지니어 수를 바꿔가며 완료율 비교
- **엔지니어 병목**: 인원을 늘렸을 때 완료율이 얼마나 오르는지(투입 대비 효과)
- **MoveGuard 연계**: 진단이 HIGH면 실패확률·지연이 커져 정비창을 못 맞추는 것을 정량 확인

예(자산 6·정비창 6h·HIGH):

| 엔지니어 | 평균 소요 | 정비창 완료율 |
| --- | --- | --- |
| 2명 | 8.4h | 0.6% |
| 3명 | 6.0h | 51% |
| 4명 | 5.2h | 87% |
