"""
전환(cutover) 과정 이산사건 시뮬레이션 (SimPy).

자산(서버) N개가 한정된 엔지니어 E명을 공유하며 전환 단계를 거친다.
  백업 → 서비스중단 → 데이터이전(용량 의존) → 재설정 → 검증(성공/실패) → (실패 시 롤백)
위험 수준(LOW/MEDIUM/HIGH)이 검증 실패확률·재설정 지연을 좌우한다.
이 위험 수준은 MoveGuard 진단의 riskLevel과 연결할 수 있다(--fetch).

여러 번(replication) 돌려 정비창 완료율·소요시간·롤백률·엔지니어 가동률의 분포를 추정한다.

사용:
    python cutover_sim.py --assets 6 --engineers 2 --window-hours 6 --risk HIGH --reps 500
    python cutover_sim.py --fetch --project 1   # MoveGuard 진단 riskLevel을 위험 수준으로 사용
"""
import argparse
import random
import statistics
import urllib.request

import simpy

# 위험 수준 → (검증 실패확률, 재설정 지연 배수)
RISK = {
    "LOW": (0.05, 1.0),
    "MEDIUM": (0.20, 1.3),
    "HIGH": (0.45, 1.7),
}


def tri(rng, low, mode, high):
    return rng.triangular(low, high, mode)


def cutover(env, name, engineers, rng, cfg, result):
    """한 자산의 전환 프로세스"""
    with engineers.request() as req:
        yield req  # 엔지니어 확보까지 대기(자원 경합 → 병목)
        start = env.now

        yield env.timeout(tri(rng, 15, 25, 50))            # 백업
        yield env.timeout(tri(rng, 3, 5, 10))              # 서비스 중단
        data_gb = rng.uniform(20, 200)
        yield env.timeout(data_gb / 2.0)                   # 데이터 이전 (~2GB/분)
        yield env.timeout(tri(rng, 10, 20, 40) * cfg["reconfig_mult"])  # 재설정

        if rng.random() < cfg["p_fail"]:                   # 검증 실패 → 롤백
            yield env.timeout(tri(rng, 30, 45, 90))
            result["fail"] += 1
        else:
            result["success"] += 1

        result["busy"] += env.now - start


def run_once(cfg, seed):
    rng = random.Random(seed)
    env = simpy.Environment()
    engineers = simpy.Resource(env, capacity=cfg["engineers"])
    result = {"success": 0, "fail": 0, "busy": 0.0}

    for i in range(cfg["assets"]):
        env.process(cutover(env, f"asset{i}", engineers, rng, cfg, result))
    env.run()

    makespan = env.now  # 모든 자산 완료 시각(분)
    capacity_minutes = cfg["engineers"] * makespan
    util = result["busy"] / capacity_minutes if capacity_minutes else 0.0
    return {
        "makespan_h": makespan / 60.0,
        "within_window": makespan <= cfg["window_hours"] * 60,
        "rollbacks": result["fail"],
        "utilization": util,
    }


def fetch_risk(project):
    url = f"http://localhost:8080/api/projects/{project}/diagnoses"
    req = urllib.request.Request(url, method="POST")
    with urllib.request.urlopen(req, timeout=15) as resp:
        import json
        data = json.load(resp)
    return data.get("riskLevel", "HIGH")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assets", type=int, default=6)
    ap.add_argument("--engineers", type=int, default=2)
    ap.add_argument("--window-hours", type=float, default=6.0)
    ap.add_argument("--risk", choices=list(RISK), default="HIGH")
    ap.add_argument("--reps", type=int, default=500)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--fetch", action="store_true", help="MoveGuard 진단 riskLevel 사용")
    ap.add_argument("--project", type=int, default=1)
    args = ap.parse_args()

    risk = fetch_risk(args.project) if args.fetch else args.risk
    p_fail, reconfig_mult = RISK.get(risk, RISK["HIGH"])
    cfg = {
        "assets": args.assets, "engineers": args.engineers,
        "window_hours": args.window_hours, "p_fail": p_fail, "reconfig_mult": reconfig_mult,
    }

    runs = [run_once(cfg, args.seed + r) for r in range(args.reps)]
    makespans = [x["makespan_h"] for x in runs]
    within = sum(x["within_window"] for x in runs) / len(runs)
    rollbacks = [x["rollbacks"] for x in runs]
    util = statistics.mean(x["utilization"] for x in runs)

    print(f"위험수준 {risk}  (검증 실패확률 {p_fail:.2f}, 재설정 지연 x{reconfig_mult})")
    print(f"자산 {args.assets}대, 엔지니어 {args.engineers}명, 정비창 {args.window_hours}시간, "
          f"{args.reps}회 반복\n")
    print(f"평균 소요시간   {statistics.mean(makespans):.2f} h "
          f"(중앙값 {statistics.median(makespans):.2f}, 최대 {max(makespans):.2f})")
    print(f"정비창 내 완료율 {within * 100:.1f} %")
    print(f"평균 롤백        {statistics.mean(rollbacks):.2f} 건 / {args.assets}대")
    print(f"엔지니어 가동률  {util * 100:.1f} %")


if __name__ == "__main__":
    main()
