package cn.aioi.problem.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemServiceTagValidationTest {
    private final TagCatalogService catalog = new TagCatalogService();

    @Test
    void acceptsOnlyStandardSecondLevelTagsForManualInput() {
        assertThat(ProblemService.sanitizeTags(Set.of("最短路", "模拟"), catalog))
                .containsExactlyInAnyOrder("最短路", "模拟");
    }

    @Test
    void rejectsCategoryUnknownAndBlankTagsForManualInput() {
        assertThatThrownBy(() -> ProblemService.sanitizeTags(new LinkedHashSet<>(List.of("图论", "未知标签", " ")), catalog))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知标签：图论、未知标签、空标签");
    }
}
