package cn.aioi.problem.service;

final class ProblemTextNormalizer {
    private static final String WOWO = "\u8717\u8717";
    private static final String CANONICAL_NAME = "BOB";

    private ProblemTextNormalizer() {
    }

    static String normalizeNames(String value) {
        return value == null ? null : value.replace(WOWO, CANONICAL_NAME);
    }

    static String normalizeNamesAndTrim(String value) {
        String normalized = normalizeNames(value);
        return normalized == null ? "" : normalized.trim();
    }
}
