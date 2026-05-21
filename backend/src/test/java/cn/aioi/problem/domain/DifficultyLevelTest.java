package cn.aioi.problem.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyLevelTest {
    @Test
    void parsesChineseLabelsAndEnumNames() {
        assertThat(DifficultyLevel.fromLabelOrName("入门")).isEqualTo(DifficultyLevel.ENTRY);
        assertThat(DifficultyLevel.fromLabelOrName("CSPJ中等")).isEqualTo(DifficultyLevel.CSPJ_MEDIUM);
        assertThat(DifficultyLevel.fromLabelOrName("NOI_HELL")).isEqualTo(DifficultyLevel.NOI_HELL);
    }

    @Test
    void fallsBackToEasyForUnknownValues() {
        assertThat(DifficultyLevel.fromLabelOrName("未知")).isEqualTo(DifficultyLevel.EASY);
        assertThat(DifficultyLevel.fromLabelOrName(null)).isEqualTo(DifficultyLevel.EASY);
    }
}

