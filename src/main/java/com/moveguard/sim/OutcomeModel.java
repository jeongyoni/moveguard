package com.moveguard.sim;

import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 시나리오의 "실제 이전 성패"를 확률적으로 결정한다 (시뮬레이션의 정답 신호).
 *
 * <p>규칙(RPN)과 <b>일부러 다른 가중치</b>를 쓰고 노이즈를 더한다. 그래야:
 * <ul>
 *   <li>규칙이 예측력을 갖되(상관 있음) 정답과 완전히 일치하진 않고,
 *   <li>규칙이 놓치거나 과잉 반응하는 패턴을 학습 모델이 배울 여지가 생긴다.
 * </ul>
 * 예: 평문 프로토콜(PORT-02)은 보안 이슈지만 전환 기동 실패와는 무관하게 둔다.
 */
@Component
public class OutcomeModel {

    public enum Outcome { SUCCESS, FAIL }

    /** 위험 요인이 없으면 대부분 성공하도록 낮은 기본 로짓에서 시작 */
    private static final double BASE_LOGIT = -2.5;
    private static final double NOISE_STDDEV = 0.8;

    public Outcome sample(Map<String, String> features, Random rnd) {
        double logit = failureLogit(features) + rnd.nextGaussian() * NOISE_STDDEV;
        double failureProb = 1.0 / (1.0 + Math.exp(-logit));
        return rnd.nextDouble() < failureProb ? Outcome.FAIL : Outcome.SUCCESS;
    }

    /** 생성 파라미터로 실패 로짓을 계산 (가중치는 규칙 RPN과 독립적으로 설계) */
    double failureLogit(Map<String, String> f) {
        double x = BASE_LOGIT;

        boolean ipChanges = bool(f, "ipChanges");
        // 하드코딩 IP + 공인IP 변경 → 접속 주소가 깨져 기동 실패 가능성 큼
        if (ipChanges && bool(f, "hardcoded")) {
            x += 2.0;
        }
        // 공인IP는 바뀌는데 이전 후 DNS 계획이 없음 → 접속 불가
        if (ipChanges && !bool(f, "dnsAfter")) {
            x += 1.2;
        }
        // 외부 허용목록 IP 변경 → 방화벽 등록 지연으로 전환 차질
        if (ipChanges && bool(f, "whitelisted")) {
            x += 0.9;
        }
        // Tomcat 9↓ → 10↑ : javax→jakarta로 애플리케이션이 기동 안 됨 (강한 실패 요인)
        if (major(f.get("tomcatFrom")) <= 9 && major(f.get("tomcatTo")) >= 10) {
            x += 1.8;
        }
        // 목표 WAS 최소 Java 미달 → 기동 실패
        if (javaBelowMinimum(f)) {
            x += 1.6;
        }
        // 인증서가 임박 만료인데 갱신 계획 없음 → HTTPS 접속 실패
        if (bool(f, "certExpiring") && !bool(f, "certAfter")) {
            x += 1.0;
        }
        // 이전 후 DB가 EOL 버전 → 당장 기동은 되지만 운영 위험 (작은 기여)
        if ("9.6".equals(f.get("dbTo"))) {
            x += 0.4;
        }
        // 평문 프로토콜(PORT-02)은 보안 이슈일 뿐 기동 실패와 무관 → 기여 0 (의도적)
        return x;
    }

    /** tomcatTo가 요구하는 최소 Java를 javaAfter가 못 맞추는지 (11.0→17, 10.1→11 필요) */
    private boolean javaBelowMinimum(Map<String, String> f) {
        int required = "11.0".equals(f.get("tomcatTo")) ? 17 : 11;
        return major(f.get("javaAfter")) < required;
    }

    private static boolean bool(Map<String, String> f, String key) {
        return Boolean.parseBoolean(f.get(key));
    }

    private static int major(String releaseLine) {
        if (releaseLine == null || releaseLine.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(releaseLine.trim().split("\\.")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
