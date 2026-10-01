"""
전환(cutover) 과정 이산사건 시뮬레이션 — 애니메이션 버전 (Salabim).

cutover_sim.py(SimPy)와 같은 모델을, 엔지니어 대기열이 시각적으로 쌓이는
애니메이션으로 보여준다. 맥에서 바로 실행되며 화면 녹화(⌘⇧5)로 발표 영상에 쓴다.

사용:
    python cutover_anim.py --assets 6 --engineers 2 --risk HIGH
    python cutover_anim.py --assets 6 --engineers 4 --risk LOW
    python cutover_anim.py --headless     # 애니메이션 없이 숫자만(검증용)
"""
import argparse

import salabim as sim

RISK = {
    "LOW": (0.05, 1.0),
    "MEDIUM": (0.20, 1.3),
    "HIGH": (0.45, 1.7),
}


class Stats:
    success = 0
    fail = 0


class Asset(sim.Component):
    def setup(self, engineers, cfg):
        self.engineers = engineers
        self.cfg = cfg

    def process(self):
        yield self.request(self.engineers)                      # 엔지니어 대기 → 확보
        yield self.hold(sim.Triangular(15, 50, 25).sample())    # 백업
        yield self.hold(sim.Triangular(3, 10, 5).sample())      # 서비스 중단
        yield self.hold(sim.Uniform(20, 200).sample() / 2.0)    # 데이터 이전
        yield self.hold(sim.Triangular(10, 40, 20).sample() * self.cfg["mult"])  # 재설정
        if sim.Uniform(0, 1).sample() < self.cfg["p_fail"]:     # 검증 실패 → 롤백
            yield self.hold(sim.Triangular(30, 90, 45).sample())
            Stats.fail += 1
        else:
            Stats.success += 1
        self.release()


def build(args):
    p_fail, mult = RISK[args.risk]
    cfg = {"p_fail": p_fail, "mult": mult}

    env = sim.Environment(time_unit="minutes", yieldless=False)
    engineers = sim.Resource("엔지니어", capacity=args.engineers)
    for i in range(args.assets):
        Asset(name=f"자산{i + 1}", engineers=engineers, cfg=cfg)

    if not args.headless:
        env.animate(True)
        env.modelname("MoveGuard 전환 시뮬레이션")
        env.speed(8)
        env.background_color("20%gray")
        sim.AnimateText(text=f"위험수준 {args.risk}  |  엔지니어 {args.engineers}명  |  자산 {args.assets}대",
                        x=10, y=680, fontsize=18, textcolor="white")
        sim.AnimateText(text=lambda: f"경과 {env.now() / 60:.1f}h   성공 {Stats.success}   롤백 {Stats.fail}",
                        x=10, y=650, fontsize=16, textcolor="white")
        sim.AnimateText(text="엔지니어 배정 대기열", x=500, y=360, fontsize=14, textcolor="white")
        sim.AnimateText(text="전환 작업 중", x=120, y=360, fontsize=14, textcolor="white")
        sim.AnimateQueue(engineers.claimers(), x=120, y=330, direction="s", title="")
        sim.AnimateQueue(engineers.requesters(), x=500, y=330, direction="s", title="")
    return env


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assets", type=int, default=6)
    ap.add_argument("--engineers", type=int, default=2)
    ap.add_argument("--risk", choices=list(RISK), default="HIGH")
    ap.add_argument("--headless", action="store_true")
    args = ap.parse_args()

    Stats.success = 0
    Stats.fail = 0
    env = build(args)
    env.run()

    print(f"위험수준 {args.risk}, 엔지니어 {args.engineers}명, 자산 {args.assets}대")
    print(f"총 소요시간 {env.now() / 60:.2f}h, 성공 {Stats.success}, 롤백 {Stats.fail}")


if __name__ == "__main__":
    main()
