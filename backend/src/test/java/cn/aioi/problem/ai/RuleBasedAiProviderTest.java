package cn.aioi.problem.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAiProviderTest {
    @Test
    void raisesDifficultyForDpAndLargeConstraintsWithStandardTags() {
        RuleBasedAiProvider provider = new RuleBasedAiProvider();

        AiAssessment assessment = provider.assess(new ProblemInput("区间问题", "n <= 10^5，需要动态规划和状态转移优化"));

        assertThat(assessment.tags()).contains("线性 DP");
        assertThat(assessment.tags()).doesNotContain("动态规划", "图论");
        assertThat(assessment.difficulty().rank()).isGreaterThanOrEqualTo(3);
        assertThat(assessment.hints()).hasSize(3);
    }
}
