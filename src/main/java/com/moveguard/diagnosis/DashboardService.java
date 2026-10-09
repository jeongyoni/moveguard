package com.moveguard.diagnosis;

import com.moveguard.project.Project;
import com.moveguard.project.ProjectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 대시보드(모니터링 콘솔)의 전체 현황 집계를 만든다. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectService projectService;
    private final DiagnosisMapper mapper;

    public record Card(Project project, RunSummary latest) {
        public boolean hasRun() {
            return latest != null;
        }

        public String level() {
            return latest != null ? latest.getRiskLevel() : "미진단";
        }
    }

    public record View(List<Card> cards, int total, int high, int medium, int low, int untested,
                       int blocked, int totalFindings, int avgMaxRpn, List<Card> recent) {
    }

    public View build() {
        List<Project> projects = projectService.findProjects();
        List<Card> cards = new ArrayList<>();
        for (Project p : projects) {
            cards.add(new Card(p, mapper.findLatestRun(p.getProjectId())));
        }
        int high = 0, medium = 0, low = 0, untested = 0, blocked = 0, findings = 0, rpnSum = 0, rpnN = 0;
        for (Card c : cards) {
            RunSummary r = c.latest();
            if (r == null) {
                untested++;
                continue;
            }
            switch (r.getRiskLevel()) {
                case "HIGH" -> high++;
                case "MEDIUM" -> medium++;
                default -> low++;
            }
            if (r.isBlocked()) {
                blocked++;
            }
            findings += r.getFindingCount();
            rpnSum += r.getMaxRpn();
            rpnN++;
        }
        int avg = rpnN > 0 ? rpnSum / rpnN : 0;
        List<Card> recent = cards.stream()
                .filter(Card::hasRun)
                .sorted(Comparator.comparing((Card c) -> c.latest().getExecutedAt()).reversed())
                .limit(6)
                .toList();
        return new View(cards, projects.size(), high, medium, low, untested, blocked, findings, avg, recent);
    }
}
