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
    @DisplayName("생성된 컨텍스트는 web·db 자산과 의존관계를 포함한다")
    void producesValidContext() {
        Scenario s = generator.generate(new Random(1L), List.of());

        assertThat(s.context().dependencies()).hasSize(1);
        assertThat(s.context().software()).isNotEmpty();
        assertThat(s.features()).containsKeys("ipChanges", "whitelisted", "tomcatFrom", "dbTo");
    }
}
