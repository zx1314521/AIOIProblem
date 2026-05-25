package cn.aioi.problem.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagCatalogServiceTest {
    private final TagCatalogService catalog = new TagCatalogService();

    @Test
    void exposesCategoriesButDoesNotTreatCategoriesAsTags() {
        assertThat(catalog.categories())
                .anySatisfy(category -> {
                    assertThat(category.name()).isEqualTo("图论");
                    assertThat(category.tags()).contains("最短路", "网络流");
                });

        assertThat(catalog.isCategory("图论")).isTrue();
        assertThat(catalog.isStandardTag("图论")).isFalse();
        assertThat(catalog.isStandardTag("最短路")).isTrue();
    }

    @Test
    void normalizesAliasesPunctuationCaseWhitespaceAndDuplicates() {
        assertThat(catalog.normalizeTags(List.of(" BFS ", "广搜", "DP", "Segment Tree", "线段树 Beats", "图论", "未知标签")))
                .containsExactly("广度优先搜索 BFS", "线段树", "吉司机线段树 segment tree beats");
    }

    @Test
    void normalizesCommonContestAliases() {
        assertThat(catalog.normalizeTags(List.of("IDA*", "A*", "MITM", "数位动态规划", "状态压缩 DP", "2sat")))
                .containsExactly(
                        "启发式迭代加深搜索 IDA*",
                        "A* 算法",
                        "折半搜索 meet in the middle",
                        "数位 DP",
                        "状压 DP",
                        "2-SAT"
                );
    }

    @Test
    void fallsBackToSimulationWhenAiTagsHaveNoStandardMatch() {
        assertThat(catalog.normalizeAiTags(List.of("DP", "图论", "unknown"))).containsExactly("模拟");
    }

    @Test
    void promptTextListsOnlyStandardTagsGroupedByCategory() {
        String prompt = catalog.promptText();

        assertThat(prompt).contains("一级分类不可作为标签");
        assertThat(prompt).contains("图论：Kruskal 重构树、网络流");
        assertThat(prompt).contains("基础算法：模拟、贪心");
    }
}
