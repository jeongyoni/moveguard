package com.moveguard.compat;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * endoflife.date의 cycle JSON을 CompatRelease로 변환한다.
 * eol / lts / extendedSupport 필드는 boolean 또는 날짜 문자열로 오는데, 이를 일관되게 해석한다.
 */
final class CompatReleaseFactory {

    private CompatReleaseFactory() {
    }

    static CompatRelease fromCycle(String product, JsonNode cycle, LocalDate today) {
        String version = text(cycle.path("cycle"));

        JsonNode eolNode = cycle.path("eol");
        LocalDate eolDate = asDate(eolNode);
        boolean eol = eolNode.isBoolean()
                ? eolNode.asBoolean()
                : (eolDate != null && !eolDate.isAfter(today));

        JsonNode ltsNode = cycle.path("lts");
        // lts가 날짜 문자열이면 "그 시점부터 LTS"라는 의미이므로 LTS로 본다
        boolean lts = ltsNode.isBoolean() ? ltsNode.asBoolean() : ltsNode.isTextual();

        LocalDate extSupport = asDate(cycle.path("extendedSupport"));
        LocalDate releaseDate = asDate(cycle.path("releaseDate"));
        String latest = text(cycle.path("latest"));
        String minJava = text(cycle.path("minJavaVersion"));
        String label = version + (lts ? " (LTS)" : "");

        return new CompatRelease(null, product, version, label, releaseDate,
                lts, eol, eolDate, extSupport, !eol, latest, minJava, false);
    }

    private static String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static LocalDate asDate(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        try {
            return LocalDate.parse(node.asText());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
