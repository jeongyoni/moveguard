"""
전환(cutover) 과정 이산사건 시뮬레이션 — 애니메이션 버전 (Salabim).

cutover_sim.py(SimPy)와 같은 모델을, 자산이 엔지니어를 기다리며 줄 서고
각 전환 단계를 거치는 모습으로 보여준다. 발표 영상용.

화면에 보이는 것
  - 대기 / 작업 중 / 완료 세 칸과, 작업 중인 자산의 **현재 단계**
  - 정비창(기본 6시간) 진행 바 — 넘기면 빨강으로 바뀐다
  - 경과 시간, 성공·롤백 수, 엔지니어 가동 현황

사용:
    python cutover_anim.py --assets 6 --engineers 2 --risk HIGH
    python cutover_anim.py --assets 6 --engineers 4 --risk LOW
    python cutover_anim.py --video out.mp4            # 화면 녹화 없이 mp4로 저장
    python cutover_anim.py --snapshots 60,180,360     # 해당 시각(분) 화면을 png로 저장
    python cutover_anim.py --headless                 # 애니메이션 없이 숫자만(검증용)
"""
import argparse
import sys

import salabim as sim

# 위험 수준 → (검증 실패확률, 재설정 지연 배수) — cutover_sim.py와 동일
RISK = {
    "LOW": (0.05, 1.0),
    "MEDIUM": (0.20, 1.3),
    "HIGH": (0.45, 1.7),
}

# 단계별 색 (범례와 자산 블록에 같이 쓴다)
STAGE_COLORS = {
    "대기": "#64748b",
    "백업": "#3b82f6",
    "서비스중단": "#f59e0b",
    "데이터이전": "#8b5cf6",
    "재설정": "#06b6d4",
    "검증": "#eab308",
    "롤백": "#ef4444",
    "성공": "#22c55e",
}
STAGE_ORDER = ["백업", "서비스중단", "데이터이전", "재설정", "검증", "롤백"]

COL_W = 250          # 자산 블록 너비
BAR_X, BAR_W = 560, 600
BAR_SCALE_MIN = 720  # 진행 바 전체 = 12시간

FONT = ""            # main에서 지정 (윈도우 malgun / 맥 AppleGothic)


class Stats:
    success = 0
    fail = 0


class Asset(sim.Component):
    def setup(self, engineers, cfg, done):
        self.engineers = engineers
        self.cfg = cfg
        self.done = done
        self.stage = "대기"

    def animation_objects(self, id):
        """AnimateQueue가 자산 한 개를 그리는 방법 — 이름 + 현재 단계"""
        block = sim.AnimateRectangle(
            spec=(0, 0, COL_W - 20, 32),
            fillcolor=lambda: STAGE_COLORS.get(self.stage, "#64748b"),
            linecolor="white",
            linewidth=1,
            text=lambda: f"{self.name()}   {self.stage}",
            fontsize=15,
            textcolor="white",
            font=FONT,
        )
        return COL_W, 40, block

    def process(self):
        yield self.request(self.engineers)                      # 엔지니어 대기 → 확보

        self.stage = "백업"
        yield self.hold(sim.Triangular(15, 50, 25).sample())
        self.stage = "서비스중단"
        yield self.hold(sim.Triangular(3, 10, 5).sample())
        self.stage = "데이터이전"
        yield self.hold(sim.Uniform(20, 200).sample() / 2.0)
        self.stage = "재설정"
        yield self.hold(sim.Triangular(10, 40, 20).sample() * self.cfg["mult"])

        self.stage = "검증"
        if sim.Uniform(0, 1).sample() < self.cfg["p_fail"]:     # 검증 실패 → 롤백
            self.stage = "롤백"
            yield self.hold(sim.Triangular(30, 90, 45).sample())
            Stats.fail += 1
        else:
            self.stage = "성공"
            Stats.success += 1

        self.release()
        self.enter(self.done)


def draw(env, args, engineers, done, window_min):
    """화면 구성 — 제목·시계·정비창 진행 바·세 칸·범례"""
    env.animate(True)
    # x0/y0/x1을 창 크기와 같게 둬야 좌표 1 = 픽셀 1 (기본 x1=1024라 그냥 두면 확대된다)
    env.animation_parameters(width=1280, height=720, x0=0, y0=0, x1=1280,
                             title="MoveGuard 전환 시뮬레이션", fps=args.fps,
                             show_time=False)
    env.modelname("")
    env.background_color("#0f172a")
    env.speed(args.speed)

    def over():
        return env.now() > window_min

    # 제목
    sim.AnimateText(text="MoveGuard — 전환(cutover) 과정 이산사건 시뮬레이션",
                    x=40, y=672, fontsize=24, textcolor="white", font=FONT)
    sim.AnimateText(text=f"위험수준 {args.risk}  ·  엔지니어 {args.engineers}명  ·  "
                         f"자산 {args.assets}대  ·  정비창 {window_min / 60:.0f}시간",
                    x=40, y=640, fontsize=15, textcolor="#94a3b8", font=FONT)

    # 경과 시간 · 결과
    sim.AnimateText(text=lambda: f"경과 {env.now() / 60:.1f}h",
                    x=40, y=586, fontsize=30, textcolor="white", font=FONT)
    sim.AnimateText(text=lambda: f"성공 {Stats.success}   롤백 {Stats.fail}   "
                                 f"엔지니어 {engineers.claimed_quantity():.0f}/{args.engineers} 작업 중",
                    x=230, y=592, fontsize=15, textcolor="#cbd5e1", font=FONT)

    # 정비창 진행 바 (전체 = 12시간, 정비창 지점에 눈금)
    sim.AnimateRectangle(spec=(BAR_X, 586, BAR_X + BAR_W, 610),
                         fillcolor="#1e293b", linecolor="#475569", linewidth=1)
    sim.AnimateRectangle(
        spec=lambda: (BAR_X, 586,
                      BAR_X + min(env.now(), BAR_SCALE_MIN) / BAR_SCALE_MIN * BAR_W, 610),
        fillcolor=lambda: "#ef4444" if over() else "#22c55e", linewidth=0)
    mark_x = BAR_X + window_min / BAR_SCALE_MIN * BAR_W
    sim.AnimateLine(spec=(mark_x, 580, mark_x, 616), linecolor="white", linewidth=2)
    sim.AnimateText(text=f"정비창 {window_min / 60:.0f}h", x=mark_x + 6, y=618,
                    fontsize=13, textcolor="white", font=FONT)
    sim.AnimateText(text=lambda: f"정비창 초과 +{(env.now() - window_min) / 60:.1f}h" if over() else "",
                    x=BAR_X, y=560, fontsize=15, textcolor="#ef4444", font=FONT)

    # 세 칸
    cols = [
        (40, "엔지니어 배정 대기", engineers.requesters(), "#f59e0b"),
        (450, "전환 작업 중", engineers.claimers(), "#38bdf8"),
        (860, "완료", done, "#22c55e"),
    ]
    for x, label, q, color in cols:
        sim.AnimateText(text=label, x=x, y=502, fontsize=17, textcolor=color, font=FONT)
        sim.AnimateText(text=lambda q=q: f"{len(q)}대", x=x + COL_W - 60, y=502,
                        fontsize=17, textcolor="#94a3b8", font=FONT)
        sim.AnimateLine(spec=(x, 494, x + COL_W - 20, 494), linecolor="#334155", linewidth=1)
        sim.AnimateQueue(q, x=x, y=450, direction="s", title="")

    # 지금 무슨 일이 벌어지는지 한 줄 — 발표 때 이 줄을 읽으면 된다
    def status():
        waiting = len(engineers.requesters())
        if len(done) == args.assets:
            verdict = "정비창 안에 완료" if env.now() <= window_min else "정비창 초과"
            return f"전환 종료 — {env.now() / 60:.1f}h, 성공 {Stats.success} · 롤백 {Stats.fail} → {verdict}"
        if waiting:
            return f"엔지니어 {args.engineers}명이 모두 작업 중 → {waiting}대가 배정을 기다린다 (병목)"
        return "대기 없음 — 모든 자산에 엔지니어가 배정됨"

    sim.AnimateText(text=status, x=40, y=170, fontsize=19, font=FONT,
                    textcolor=lambda: "#ef4444" if len(engineers.requesters()) else "#e2e8f0")

    # 범례
    sim.AnimateText(text="전환 단계", x=40, y=96, fontsize=14, textcolor="#94a3b8", font=FONT)
    for i, stage in enumerate(STAGE_ORDER):
        x = 40 + i * 180
        sim.AnimateRectangle(spec=(x, 56, x + 16, 72), fillcolor=STAGE_COLORS[stage], linewidth=0)
        sim.AnimateText(text=stage, x=x + 24, y=57, fontsize=14, textcolor="white", font=FONT)


def build(args, window_min):
    p_fail, mult = RISK[args.risk]
    cfg = {"p_fail": p_fail, "mult": mult}

    # 영상·스냅샷을 만들 때는 창을 띄우지 않는다(blind animation).
    # 창을 띄우면 중간에 닫힐 때 렌더링이 끊기고, 실시간 속도에 묶여 더 느리다.
    env = sim.Environment(time_unit="minutes", yieldless=False, random_seed=args.seed,
                          blind_animation=bool(args.video or args.snapshots))
    engineers = sim.Resource("엔지니어", capacity=args.engineers)
    done = sim.Queue("완료")
    for i in range(args.assets):
        Asset(name=f"자산{i + 1}", engineers=engineers, cfg=cfg, done=done)

    if not args.headless:
        draw(env, args, engineers, done, window_min)
    return env


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assets", type=int, default=6)
    ap.add_argument("--engineers", type=int, default=2)
    ap.add_argument("--risk", choices=list(RISK), default="HIGH")
    ap.add_argument("--window-hours", type=float, default=6.0)
    ap.add_argument("--speed", type=float, default=8.0, help="실시간 1초당 시뮬레이션 분")
    ap.add_argument("--fps", type=float, default=30.0)
    ap.add_argument("--seed", type=int, default=42, help="같은 영상을 다시 뽑으려면 고정")
    ap.add_argument("--video", help="mp4로 저장 (화면 녹화 불필요)")
    ap.add_argument("--snapshots", help="png로 남길 시각(분), 쉼표 구분 — 예: 60,180,360")
    ap.add_argument("--font", default="malgun" if sys.platform == "win32" else "AppleGothic",
                    help="한글 글꼴 (윈도우 malgun, 맥 AppleGothic)")
    ap.add_argument("--headless", action="store_true")
    args = ap.parse_args()

    global FONT
    FONT = args.font

    Stats.success = 0
    Stats.fail = 0
    window_min = args.window_hours * 60
    env = build(args, window_min)

    if args.snapshots and not args.headless:
        for i, at in enumerate(float(t) for t in args.snapshots.split(",")):
            env.run(till=at)
            path = f"snapshot_{i:02d}_{int(at):04d}min.png"
            env.snapshot(path)
            print(f"저장 {path}  (t={at:.0f}분)")
        env.run()
    elif args.video:
        with env.video(args.video):
            env.run()
        print(f"저장 {args.video}")
    else:
        env.run()

    within = "예" if env.now() <= window_min else "아니오"
    print(f"위험수준 {args.risk}, 엔지니어 {args.engineers}명, 자산 {args.assets}대")
    print(f"총 소요시간 {env.now() / 60:.2f}h, 성공 {Stats.success}, 롤백 {Stats.fail}, "
          f"정비창({args.window_hours}h) 내 완료 {within}")


if __name__ == "__main__":
    main()
