package com.moveguard.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 지원종료(EOL) 기준 데이터를 사람이 보는 화면. 외부 공개 데이터와 동기화해 유지된다. */
@Controller
@RequiredArgsConstructor
public class CompatViewController {

    private final CompatMapper compatMapper;

    /** 카테고리 표시 순서·한글 라벨 */
    private static final List<String> CAT_ORDER = List.of("os", "database", "server-app", "lang");

    private static String catLabel(String c) {
        return switch (c == null ? "" : c) {
            case "os" -> "운영체제";
            case "database" -> "데이터베이스";
            case "server-app" -> "WAS · 서버앱";
            case "lang" -> "언어 · 런타임";
            default -> "기타";
        };
    }

    public record ProductGroup(String product, String label, String category, String categoryLabel,
                               int total, long eolCount, List<CompatRelease> releases) {
    }

    @GetMapping("/eol")
    public String eol(Model model) {
        List<CompatRelease> releases = compatMapper.findAllReleases();
        Map<String, CompatProduct> meta = compatMapper.findProducts().stream()
                .collect(Collectors.toMap(CompatProduct::getProduct, Function.identity(), (a, b) -> a));

        // 제품별로 묶고, 그룹 내 버전은 지원중 먼저·버전 내림차순
        Map<String, List<CompatRelease>> byProduct = new LinkedHashMap<>();
        for (CompatRelease r : releases) {
            byProduct.computeIfAbsent(r.getProduct(), k -> new ArrayList<>()).add(r);
        }
        List<ProductGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<CompatRelease>> e : byProduct.entrySet()) {
            String product = e.getKey();
            List<CompatRelease> rels = e.getValue();
            rels.sort(Comparator.comparing(CompatRelease::isEol)
                    .thenComparing(CompatRelease::getVersion, Comparator.reverseOrder()));
            CompatProduct m = meta.get(product);
            String label = m != null && m.getLabel() != null ? m.getLabel() : product;
            String category = m != null ? m.getCategory() : "기타";
            long eol = rels.stream().filter(CompatRelease::isEol).count();
            groups.add(new ProductGroup(product, label, category, catLabel(category), rels.size(), eol, rels));
        }
        // 카테고리 순 → 제품 라벨 순
        groups.sort(Comparator
                .comparingInt((ProductGroup g) -> {
                    int i = CAT_ORDER.indexOf(g.category());
                    return i < 0 ? CAT_ORDER.size() : i;
                })
                .thenComparing(ProductGroup::label, String.CASE_INSENSITIVE_ORDER));

        long eolCount = releases.stream().filter(CompatRelease::isEol).count();
        model.addAttribute("groups", groups);
        model.addAttribute("products", compatMapper.findProducts());
        model.addAttribute("drivers", compatMapper.findDriverRequirements());
        model.addAttribute("total", releases.size());
        model.addAttribute("eolCount", eolCount);
        return "eol";
    }
}
