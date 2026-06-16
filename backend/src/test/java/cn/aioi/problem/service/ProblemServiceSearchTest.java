package cn.aioi.problem.service;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.ProblemDataSet;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemDataSetRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.ProblemSetItemRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemServiceSearchTest {
    @Test
    void searchLoadsPerProblemStateInBulk() {
        ProblemRepository problems = mock(ProblemRepository.class);
        PassedProblemRepository passedProblems = mock(PassedProblemRepository.class);
        ProblemSetItemRepository problemSetItems = mock(ProblemSetItemRepository.class);
        BatchJobItemRepository batchJobItems = mock(BatchJobItemRepository.class);
        ProblemDataSetRepository dataSets = mock(ProblemDataSetRepository.class);
        ProblemService service = new ProblemService(
                problems,
                passedProblems,
                problemSetItems,
                batchJobItems,
                dataSets,
                new TagCatalogService()
        );
        User user = new User("bulk-state", "hash");
        Problem first = new Problem("First", "statement one", DifficultyLevel.EASY, Set.of("妯℃嫙"), null, user);
        Problem second = new Problem("Second", "statement two", DifficultyLevel.ENTRY, Set.of("妯℃嫙"), null, user);
        setProblemId(first, 10L);
        setProblemId(second, 20L);
        when(problems.findAllWithTags()).thenReturn(List.of(first, second));
        when(passedProblems.findPassedProblemIdsByUser(user)).thenReturn(List.of(20L));
        when(dataSets.findStatusesByProblemIds(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, cn.aioi.problem.domain.ProblemDataStatus.READY}));

        var responses = service.search(null, null, null, List.of(), user);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting("id").containsExactly(20L, 10L);
        assertThat(responses.get(0).passed()).isTrue();
        assertThat(responses.get(0).description()).isEmpty();
        assertThat(responses.get(0).dataStatus()).isEqualTo("NONE");
        assertThat(responses.get(1).passed()).isFalse();
        assertThat(responses.get(1).description()).isEmpty();
        assertThat(responses.get(1).dataStatus()).isEqualTo("READY");
        verify(passedProblems, never()).existsByUserAndProblem(any(), any());
        verify(dataSets, never()).findStatusByProblemId(any());
    }

    private void setProblemId(Problem problem, Long id) {
        try {
            var field = Problem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(problem, id);
            var createdAt = Problem.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(problem, java.time.Instant.ofEpochMilli(id));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
