package cn.aioi.problem.service;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.Problem;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProblemTagCleanupServiceTest {
    private final ProblemTagCleanupService cleanup = new ProblemTagCleanupService(
            mock(cn.aioi.problem.repository.ProblemRepository.class),
            new TagCatalogService()
    );

    @Test
    void removesCategoriesAndUnknownTagsFromExistingProblems() {
        Problem problem = new Problem(
                "旧数据",
                "题面",
                DifficultyLevel.EASY,
                new LinkedHashSet<>(List.of("图论", "最短路径", "未知标签", "KMP")),
                "legacy",
                null
        );

        cleanup.normalizeProblemTags(problem);

        assertThat(problem.getTags()).containsExactly("最短路", "KMP 算法");
    }
}
