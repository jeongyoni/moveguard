# 진단 리포트 LLM 생성

MoveGuard 진단 결과(JSON)를 **로컬 LLM**이 고객사 보고용 한국어 리포트(.md)로 작성한다.

```
진단 API → 진단 결과 JSON → 프롬프트 → Ollama(로컬 LLM) → report.md
```

- 외부 API를 쓰지 않는다. **DGX Spark에 띄운 Ollama**를 HTTP로 호출해 GPU로 추론한다.
- 파이썬 표준 라이브러리(`urllib`)만 쓴다 — 설치할 의존성이 없다.
- 진단이 주는 건 규칙 단위의 사실(무엇이 왜 위험한지 + 조치)이고, LLM은 그걸 **읽을 수 있는 문서로 엮는 역할**만 한다.

## 1. 진단 결과 JSON 준비

앱을 띄운 상태에서:

```bash
curl -X POST http://localhost:8080/api/projects/1/diagnoses > report/sample_diagnosis.json
```

> **윈도우에서는 Git Bash나 cmd를 쓴다.** PowerShell의 리다이렉트는 응답을 콘솔 코드페이지로 다시
> 인코딩해서 한글이 깨진다(저장소에 커밋돼 있던 샘플이 실제로 이 문제로 중간부터 깨져 있었다).

이미 들어 있는 `sample_diagnosis.json`으로 바로 시작해도 된다.

## 2. 프롬프트만 확인 (LLM 없이 — 맥·윈도우 어디서나)

```bash
cd report
python generate_report.py --dry-run
```

LLM을 호출하지 않고 **프롬프트만 출력**한다. 진단 결과가 프롬프트에 제대로 들어갔는지 먼저 확인하는 용도다.

## 3. DGX에서 Ollama로 실제 생성

**Ollama 설치·기동** (DGX, 한 번만)

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &                 # 기본 포트 11434
ollama pull qwen2.5:14b        # 모델 받기 (한국어 품질이 무난한 편)
```

**리포트 생성**

```bash
cd report
python generate_report.py --diagnosis sample_diagnosis.json --model qwen2.5:14b --out report.md
```

**GPU를 쓰고 있는지 확인**

```bash
ollama ps        # PROCESSOR 열이 GPU인지
nvidia-smi       # 생성 중 GPU 사용률
```

## 옵션

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `--diagnosis` | `sample_diagnosis.json` | 진단 결과 JSON 경로 |
| `--model` | `qwen2.5:14b` | Ollama 모델 이름 |
| `--host` | `http://localhost:11434` | Ollama 주소. DGX를 원격으로 쓰면 그 주소를 넣는다 |
| `--out` | `report.md` | 저장할 리포트 경로 |
| `--dry-run` | — | 프롬프트만 출력(LLM 호출 안 함) |

## 리포트 구성

프롬프트가 아래 네 부분을 요청한다.

1. 요약 — 위험등급·차단 여부·핵심 메시지
2. 꼭 먼저 해결할 **전환 차단** 항목
3. 위험 항목별 설명과 권장 조치 (실무자용)
4. 전환 전 체크리스트

각 발견 항목은 `(규칙코드, RPN, 위험요인) 제목 / 내용 / 조치` 형태로 프롬프트에 들어간다.

## 자원

- 추론만 하므로 GPU 부담이 가볍다. 14B 모델이면 VRAM 10GB 안팎.
- 모델을 바꾸려면 `ollama pull <모델>` 후 `--model`만 바꾸면 된다. 더 작게 가려면 `llama3.1:8b`.

## 주의

- LLM 출력은 **검토 없이 그대로 고객사에 보내지 않는다.** 숫자와 규칙 판정은 진단 결과가 원본이고,
  리포트는 그것을 풀어 쓴 것이다. 생성물에 사실과 다른 내용이 섞이지 않았는지 확인해야 한다.
- 모델·날짜에 따라 문장이 매번 달라진다. 같은 문서를 재현해야 한다면 생성된 `.md`를 보관한다.
