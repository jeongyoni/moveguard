"""
MoveGuard 진단 결과(JSON)를 로컬 LLM으로 사람이 읽는 한국어 리포트(.md)로 작성한다.

DGX Spark에서 Ollama 로컬 LLM을 띄워 GPU로 추론한다(설치는 report/README.md 참고).
외부 API를 쓰지 않고, 표준 라이브러리(urllib)로 Ollama HTTP API만 호출한다.

사용:
    # 프롬프트만 확인 (LLM 없이, 어디서나)
    python generate_report.py --dry-run

    # DGX에서 Ollama로 실제 리포트 생성
    python generate_report.py --diagnosis sample_diagnosis.json --model qwen2.5:14b --out report.md
"""
import argparse
import json
import sys
import urllib.request


def build_prompt(d):
    lines = [
        "당신은 IT 시스템 이전(마이그레이션) 전문가입니다.",
        "아래 자동 진단 결과를 바탕으로, 고객사 보고용 한국어 리포트를 마크다운으로 작성하세요.",
        "",
        f"- 종합 위험등급: {d.get('riskLevel')}",
        f"- 전환 차단 여부: {'차단' if d.get('blocked') else '가능'}",
        f"- 최고 RPN: {d.get('maxRpn')}, 종합 점수: {d.get('totalScore')}",
        f"- 발견된 위험: {len(d.get('findings', []))}건",
        "",
        "발견 항목(위험 순):",
    ]
    for i, f in enumerate(d.get("findings", []), 1):
        block = " [전환 차단]" if f.get("blocking") else ""
        factor = f" / {f.get('factorName')}" if f.get("factorName") else ""
        lines.append(f"{i}. ({f.get('ruleCode')}, RPN {f.get('rpn')}{factor}){block} {f.get('title')}")
        lines.append(f"   - 내용: {f.get('message')}")
        lines.append(f"   - 조치: {f.get('mitigation')}")

    lines += [
        "",
        "리포트 구성:",
        "1. 요약 (위험등급·차단 여부·핵심 메시지 3~4문장)",
        "2. 꼭 먼저 해결할 전환 차단 항목 (있으면)",
        "3. 위험 항목별 설명과 권장 조치 (실무자용)",
        "4. 전환 전 체크리스트",
        "",
        "전문 용어는 간단한 설명을 덧붙이고, 과장 없이 사실 기반으로 작성하세요.",
    ]
    return "\n".join(lines)


def call_ollama(host, model, prompt):
    payload = json.dumps({"model": model, "prompt": prompt, "stream": False}).encode("utf-8")
    req = urllib.request.Request(
        f"{host}/api/generate", data=payload, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=600) as resp:
        return json.load(resp)["response"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--diagnosis", default="sample_diagnosis.json", help="진단 결과 JSON 경로")
    ap.add_argument("--model", default="qwen2.5:14b")
    ap.add_argument("--host", default="http://localhost:11434", help="Ollama 주소")
    ap.add_argument("--out", default="report.md")
    ap.add_argument("--dry-run", action="store_true", help="프롬프트만 출력(LLM 호출 안 함)")
    args = ap.parse_args()

    with open(args.diagnosis, encoding="utf-8") as f:
        diagnosis = json.load(f)
    prompt = build_prompt(diagnosis)

    if args.dry_run:
        # 윈도우 콘솔은 기본이 cp949라 한글 프롬프트를 그대로 출력하면 깨진다
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8")
        print(prompt)
        return

    print(f"LLM 호출: model={args.model} host={args.host} ...")
    report = call_ollama(args.host, args.model, prompt)
    with open(args.out, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"리포트 저장: {args.out} ({len(report)}자)")


if __name__ == "__main__":
    main()
