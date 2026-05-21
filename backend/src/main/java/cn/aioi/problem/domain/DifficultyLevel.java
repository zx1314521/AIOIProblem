package cn.aioi.problem.domain;

import java.util.Arrays;

public enum DifficultyLevel {
    ENTRY("入门", 1),
    EASY("简单", 2),
    CSPJ_MEDIUM("CSPJ中等", 3),
    CSPS_ADVANCED("CSPS提高", 4),
    NOIP_HARD("NOIP困难", 5),
    NOI_HELL("地狱NOI", 6);

    private final String label;
    private final int rank;

    DifficultyLevel(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public String label() {
        return label;
    }

    public int rank() {
        return rank;
    }

    public static DifficultyLevel fromLabelOrName(String value) {
        if (value == null || value.isBlank()) {
            return EASY;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(level -> level.name().equalsIgnoreCase(trimmed) || level.label.equals(trimmed))
                .findFirst()
                .orElse(EASY);
    }
}

