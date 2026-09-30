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

```bash
cd des
python3 -m venv .venv
source .venv/bin/activate
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
