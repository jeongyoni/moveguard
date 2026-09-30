package com.moveguard.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.sim.ScenarioGenerator.Scenario;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScenarioGeneratorTest {

    private final ScenarioGenerator generator = new ScenarioGenerator();

    @Test
    @DisplayName("같은 시드 스트림이면 동일한 시나리오 특징을 낸다")
    void isDeterministic() {
        Scenario a = generator.generate(new Random(42L), List.of());
        Scenario b = generator.generate(new Random(42L), List.of());

        assertThat(a.features()).isEqualTo(b.features());
    }

    @Test
    @DisplayName("한 스트림에서 여러 개 뽑으면 다양한 시나리오가 나온다")
    void variesAcrossDraws() {
        Random rnd = new Random(42L);
        long distinct = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> generator.generate(rnd, List.of()).features())
                .distinct()
                .count();

        assertThat(distinct).isGreaterThan(5);
    }

    @Test
    @DisplayName("앱 서버 1~3대 + DB로 구성되고 집계 특징을 포함한다")
    void producesValidContext() {
        Scenario s = generator.generate(new Random(1L), List.of());

        int numApps = Integer.parseInt(s.features().get("numAppServers"));
        assertThat(numApps).isBetween(1, 3);
        // 앱 서버 수만큼 의존관계(각 앱 → DB), 자산은 앱 + DB
        assertThat(s.context().dependencies()).hasSize(numApps);
        assertThat(s.context().software()).isNotEmpty();
        assertThat(s.features()).containsKeys("numAppServers", "ipChanges", "anyJavaxJump", "dbTo");
    }
}
