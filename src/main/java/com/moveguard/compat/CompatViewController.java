package com.moveguard.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 지원종료(EOL) 기준 데이터를 사람이 보는 화면. 외부 공개 데이터와 동기화해 유지된다. */
@Controller
@RequiredArgsConstructor
public class CompatViewController {

    private final CompatMapper compatMapper;

    @GetMapping("/eol")
    public String eol(Model model) {
        List<CompatRelease> releases = new ArrayList<>(compatMapper.findAllReleases());
        releases.sort(Comparator.comparing(CompatRelease::getProduct)
                .thenComparing(CompatRelease::getVersion, Comparator.reverseOrder()));
        long eolCount = releases.stream().filter(CompatRelease::isEol).count();
        model.addAttribute("releases", releases);
        model.addAttribute("products", compatMapper.findProducts());
        model.addAttribute("drivers", compatMapper.findDriverRequirements());
        model.addAttribute("total", releases.size());
        model.addAttribute("eolCount", eolCount);
        return "eol";
    }
}
