# 성패 예측 학습 (시뮬레이션 Phase 4)

시뮬레이션 데이터셋으로 이전 성패(SUCCESS/FAIL)를 예측하는 모델을 학습하고,
규칙 baseline(전환 차단 = 실패 예측)과 비교한다.

- **입력 특징만** 사용한다(생성 파라미터). 규칙 산출물(`blocked`·`findingCodes` 등)은 학습에 넣지 않는다 — "원시 특징만으로 규칙을 이길 수 있는가"를 보기 위함.
- 표(tabular) 데이터라 **CPU로 충분**하다(수 초). GPU/DGX는 이후 딥러닝·대규모 단계에서만 필요.

## 준비

```bash
cd ml
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

> Python 3.11~3.12 권장(패키지 휠 호환성). DGX(Python 3.12)에서도 동일하게 동작.

## 실행

먼저 앱을 띄워 데이터 API를 연다(다른 터미널):

```bash
./gradlew bootRun   # 프로젝트 루트에서
```

그리고 학습:

```bash
# 실행 중인 앱에서 데이터를 받아 학습 (기본)
python train.py --count 5000 --seed 42

# 또는 미리 받아둔 CSV로
curl "http://localhost:8080/api/sim/dataset?count=5000&seed=42" -o dataset.csv
python train.py --csv dataset.csv
```

## 출력 예

```
모델                     정밀도      재현율        F1
--------------------------------------------------
규칙(baseline)           0.73x     1.000     0.84x
LogisticRegression       ...       ...       ...
RandomForest             ...       ...       ...
GradientBoosting         ...       ...       ...  <- baseline 초과
```

규칙은 재현율은 높지만 과잉차단으로 정밀도가 낮다. 학습 모델이 F1에서 규칙을 넘으면
"규칙이 못 잡는 패턴을 데이터가 보완"한다는 근거가 된다.
