# 진단 리포트 LLM 생성 (DGX 로컬 LLM)

MoveGuard 진단 결과(JSON)를 **로컬 LLM**이 사람이 읽는 한국어 리포트(.md)로 작성한다.
DGX Spark의 **GPU(로컬 LLM 추론)** 를 실제로 활용하는 부분.

- 외부 API 없음 — DGX에서 **Ollama**로 모델을 돌리고, 표준 라이브러리로 HTTP 호출만.
- 진단 결과는 JSON 파일로 주고받는다(앱과 직접 연결 불필요).

## 흐름

```
MoveGuard 진단(JSON) ──scp──▶ DGX ──▶ generate_report.py ──▶ Ollama(GPU) ──▶ 리포트.md
```

## DGX 준비 (SSH 접속 후, 한 번만)

```bash
# 1) Ollama 설치 (GPU 자동 사용)
curl -fsSL https://ollama.com/install.sh | sh

# 2) 모델 받기 (한국어 양호, 128GB 메모리면 14b 여유. 더 빠르게는 7b)
ollama pull qwen2.5:14b

# 3) 서버 확인 (보통 자동 실행, 포트 11434)
ollama list
```

## 실행

```bash
# (로컬) 진단 결과를 JSON으로 저장
curl -X POST http://localhost:8080/api/projects/1/diagnoses -o diagnosis.json

# DGX로 복사
scp diagnosis.json report/generate_report.py yjy@<DGX-IP>:~/

# (DGX) 리포트 생성 — GPU로 추론
python3 generate_report.py --diagnosis diagnosis.json --model qwen2.5:14b --out report.md
```

프롬프트만 미리 보려면(LLM 없이, 어디서나):
```bash
python3 generate_report.py --dry-run
```

옵션: `--diagnosis`(입력 JSON) `--model`(Ollama 모델) `--host`(기본 http://localhost:11434) `--out`(출력 파일) `--dry-run`.

## 산출물

`report.md` — 요약(위험등급·차단), 전환 차단 항목, 위험별 설명·조치, 전환 전 체크리스트를 담은 한국어 리포트. 발표·보고서에 그대로 붙일 수 있다.

> GPU가 실제로 쓰이는 단계. `nvidia-smi`로 추론 중 사용량 확인 가능.
