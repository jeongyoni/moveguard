"""
시뮬레이션 데이터셋으로 이전 성패(outcome)를 예측하는 모델을 학습하고,
규칙 baseline(전환 차단 = 실패 예측)과 같은 테스트셋에서 비교한다.

입력 특징(생성 파라미터)만 사용한다. 규칙 산출물(blocked·findingCodes 등)은
학습에 넣지 않는다 — "원시 특징만으로 규칙을 이길 수 있는가"를 보기 위함.

사용:
    # 실행 중인 앱에서 바로 받아 학습 (기본)
    python train.py --count 5000 --seed 42
    # 또는 미리 받아둔 CSV로
    python train.py --csv dataset.csv
"""
import argparse

import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import precision_recall_fscore_support
from sklearn.model_selection import train_test_split

# 생성 파라미터(입력 특징)만 사용. 규칙 산출물 컬럼은 제외.
FEATURE_COLS = [
    "numAppServers", "ipChanges", "whitelisted", "dnsAfter", "certExpiring", "certAfter",
    "dbFrom", "dbTo", "anyHardcoded", "anyPlaintext", "anyJavaxJump", "anyJavaBelowMin",
]
CATEGORICAL = ["dbFrom", "dbTo"]
POSITIVE = "FAIL"  # 양성 클래스 = 실제 실패


def load(args):
    source = args.csv or (
        f"http://localhost:8080/api/sim/dataset?count={args.count}&seed={args.seed}"
    )
    print(f"데이터 로드: {source}")
    return pd.read_csv(source)


def to_int_bool(series):
    if series.dtype == bool:
        return series.astype(int)
    return series.map({"true": 1, "false": 0, True: 1, False: 0}).astype(int)


def prepare(df):
    y = (df["outcome"] == POSITIVE).astype(int)
    x = df[FEATURE_COLS].copy()
    for col in x.columns:
        if col not in CATEGORICAL and x[col].dtype == object:
            x[col] = to_int_bool(x[col])
    x = pd.get_dummies(x, columns=CATEGORICAL)
    rule_pred = to_int_bool(df["blocked"])  # 규칙: 차단(blocked)이면 실패로 예측
    return x, y, rule_pred


def metrics(y_true, y_pred):
    p, r, f1, _ = precision_recall_fscore_support(
        y_true, y_pred, average="binary", zero_division=0
    )
    return p, r, f1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", help="데이터셋 CSV 경로 (없으면 앱에서 받음)")
    ap.add_argument("--count", type=int, default=5000)
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    df = load(args)
    x, y, rule_pred = prepare(df)
    print(f"표본 {len(df)}개, 특징 {x.shape[1]}개, 실패율 {y.mean():.3f}")

    x_tr, x_te, y_tr, y_te, _, rule_te = train_test_split(
        x, y, rule_pred, test_size=0.3, random_state=args.seed, stratify=y
    )

    models = {
        "LogisticRegression": LogisticRegression(max_iter=1000),
        "RandomForest": RandomForestClassifier(n_estimators=200, random_state=args.seed),
        "GradientBoosting": GradientBoostingClassifier(random_state=args.seed),
    }

    print("\n{:<20} {:>9} {:>9} {:>9}".format("모델", "정밀도", "재현율", "F1"))
    print("-" * 50)
    rp, rr, rf1 = metrics(y_te, rule_te)
    print("{:<20} {:>9.3f} {:>9.3f} {:>9.3f}".format("규칙(baseline)", rp, rr, rf1))
    for name, model in models.items():
        model.fit(x_tr, y_tr)
        p, r, f1 = metrics(y_te, model.predict(x_te))
        mark = "  <- baseline 초과" if f1 > rf1 else ""
        print("{:<20} {:>9.3f} {:>9.3f} {:>9.3f}{}".format(name, p, r, f1, mark))


if __name__ == "__main__":
    main()
