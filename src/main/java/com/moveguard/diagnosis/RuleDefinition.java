package com.moveguard.diagnosis;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** risk_rule + risk_factor 조회 결과 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {

    private Long ruleId;
    private String ruleCode;
    private String factorCode;
    private BigDecimal factorWeight;
    private String title;
    private int severity;
    private int occurrence;
    private int detection;
    private boolean blocking;
    private String messageTemplate;
    private String mitigation;

    /** FMEA 위험우선순위수 = 심각도 x 발생가능성 x 검출도 (최대 1000) */
    public int rpn() {
        return severity * occurrence * detection;
    }

    /** message_template의 {키}를 params 값으로 치환 */
    public String renderMessage(Map<String, String> params) {
        String message = messageTemplate;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}
