package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.ai.ProblemInput;
import cn.aioi.problem.api.dto.RecommendationDtos;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {
    @Test
    void recommendationUsesDedicatedAiTaskProviderForPlanning() {
        ProblemRepository problems = mock(ProblemRepository.class);
        PassedProblemRepository passedProblems = mock(PassedProblemRepository.class);
        AiProvider aiProvider = mock(AiProvider.class);
        RecommendationService service = new RecommendationService(problems, passedProblems, aiProvider);
        User user = new User("student", "hash");
        Problem solved = problem(1L, "基础模拟", DifficultyLevel.EASY, Set.of("模拟"));
        Problem graph = problem(2L, "最短路训练", DifficultyLevel.CSPJ_MEDIUM, Set.of("最短路"));
        Problem dp = problem(3L, "背包训练", DifficultyLevel.CSPJ_MEDIUM, Set.of("背包 DP"));

        when(passedProblems.findByUser(user)).thenReturn(List.of(new PassedProblem(user, solved)));
        when(problems.findAllWithTags()).thenReturn(List.of(solved, graph, dp));
        when(aiProvider.assess(any(ProblemInput.class), eq(AiTaskType.RECOMMENDATION))).thenReturn(new AiAssessment(
                DifficultyLevel.CSPJ_MEDIUM,
                0.8,
                List.of("背包 DP"),
                List.of("先看状态"),
                "推荐先补 DP。"
        ));

        RecommendationDtos.RecommendationResponse response = service.recommend(user);

        assertThat(response.weakTags()).startsWith("背包 DP");
        assertThat(response.items().get(0).problem().title()).isEqualTo("背包训练");
    }

    private Problem problem(Long id, String title, DifficultyLevel difficulty, Set<String> tags) {
        Problem problem = new Problem(title, "题面", difficulty, tags, "test", null);
        ReflectionTestUtils.setField(problem, "id", id);
        return problem;
    }
}
