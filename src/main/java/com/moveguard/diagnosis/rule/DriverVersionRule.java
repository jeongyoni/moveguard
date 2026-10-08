package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Phase;
import com.moveguard.compat.DriverRequirement;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * CMP-07: 이전 후 JDBC 드라이버 버전이 목표 DB 요구치 미달.
 * 수동 관리 매트릭스(driver_requirement)를 기준으로, 목표 DB가 요구하는
 * 최소 드라이버 버전보다 낮은 드라이버를 쓰면 전환 후 접속 실패 위험이 있다.
 */
@Component
public class DriverVersionRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-07";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<DriverRequirement> requirements = context.driverRequirements();
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<AssetSoftware> after = context.software(Phase.AFTER);

        List<Finding> findings = new ArrayList<>();
        for (DriverRequirement req : requirements) {
            // 목표(AFTER) DB가 매트릭스의 제품(+라인)과 일치해야 적용
            boolean dbPresent = after.stream().anyMatch(sw ->
                    Objects.equals(sw.getProduct(), req.getDbProduct())
                            && (req.getDbReleaseLine() == null
                            || Objects.equals(sw.getReleaseLine(), req.getDbReleaseLine())));
            if (!dbPresent) {
                continue;
            }

            for (AssetSoftware drv : after) {
                if (!Objects.equals(drv.getProduct(), req.getDriverProduct())) {
                    continue;
                }
                if (Versions.compare(drv.getVersion(), req.getMinVersion()) >= 0) {
                    continue;
                }

                String assetName = context.asset(drv.getAssetId()).map(Asset::getName).orElse("-");
                String db = req.getDbReleaseLine() == null
                        ? req.getDbProduct()
                        : req.getDbProduct() + " " + req.getDbReleaseLine();
                findings.add(new Finding(CODE, drv.getAssetId(), null, Map.of(
                        "asset", assetName,
                        "driver", req.getDriverProduct(),
                        "version", drv.getVersion(),
                        "required", req.getMinVersion(),
                        "db", db)));
            }
        }
        return findings;
    }
}
