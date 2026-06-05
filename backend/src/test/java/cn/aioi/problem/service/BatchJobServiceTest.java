package cn.aioi.problem.service;

import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.BatchJobRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BatchJobServiceTest {
    @Autowired
    BatchJobService batchJobService;

    @Autowired
    ProblemService problemService;

    @Autowired
    UserRepository users;

    @Autowired
    BatchJobRepository jobs;

    @Autowired
    BatchJobItemRepository items;

    @Autowired
    ProblemRepository problems;

    @Test
    void normalizesPersistedPendingBatchItemsBeforeAnalysis() {
        User user = users.save(new User("batch-wowo", "hash"));
        BatchJob job = jobs.save(new BatchJob("legacy batch", user, 1));
        items.save(new BatchJobItem(job, "\u8717\u8717 Legacy", "\u8717\u8717 needs help. dp 10^5", 0));

        batchJobService.triggerWorker();
        var problem = waitForProblem("BOB Legacy");

        assertThat(problem.getTitle()).isEqualTo("BOB Legacy");
        assertThat(problem.getDescription()).contains("BOB needs help.");
        assertThat(problem.getDifficulty().rank()).isGreaterThanOrEqualTo(DifficultyLevel.EASY.rank());
    }

    @Test
    void recoversPersistedRunningBatchItemsOnStartup() {
        User user = users.save(new User("batch-restart", "hash"));
        BatchJob job = jobs.save(new BatchJob("interrupted batch", user, 1));
        BatchJobItem item = new BatchJobItem(job, "Restart Recovery", "graphs and shortest paths 10^5", 0);
        item.start();
        items.save(item);

        batchJobService.resumePendingWorkOnStartup();
        var problem = waitForProblem("Restart Recovery");

        assertThat(problem.getTitle()).isEqualTo("Restart Recovery");
        assertThat(problem.getDescription()).contains("shortest paths");
    }

    @Test
    void reanalyzesExistingProblemsByUpdatingOriginalProblem() {
        User user = users.save(new User("batch-reanalyze", "hash"));
        long beforeCount = problems.count();
        Problem problem = problems.save(new Problem(
                "Needs Reanalysis",
                "<p>dynamic programming dp 10^5</p>",
                DifficultyLevel.ENTRY,
                Set.of("旧标签"),
                "manual",
                user
        ));

        var queued = batchJobService.reanalyzeProblems(List.of(problem.getId()), user);
        var updated = waitForProblemUpdate(problem.getId());

        assertThat(queued.job().totalCount()).isEqualTo(1);
        assertThat(queued.items()).hasSize(1);
        assertThat(updated.getId()).isEqualTo(problem.getId());
        assertThat(updated.getDescription()).isEqualTo("dynamic programming dp 10^5");
        assertThat(updated.getDifficulty().rank()).isGreaterThan(DifficultyLevel.ENTRY.rank());
        assertThat(updated.getTags()).doesNotContain("旧标签");
        assertThat(problems.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void failsReanalysisItemWhenTargetProblemWasDeletedBeforeProcessing() {
        User user = users.save(new User("batch-reanalyze-deleted", "hash"));
        long beforeCount = problems.count();
        Problem problem = problems.save(new Problem(
                "Deleted Before Reanalysis",
                "dynamic programming dp 10^5",
                DifficultyLevel.ENTRY,
                Set.of("旧标签"),
                "manual",
                user
        ));
        BatchJob job = jobs.save(new BatchJob("重新分析题目", user, 1));
        BatchJobItem item = items.save(BatchJobItem.reanalysis(
                job,
                problem.getTitle(),
                problem.getDescription(),
                0,
                problem.getId()
        ));
        problemService.delete(problem.getId());

        batchJobService.triggerWorker();
        var failed = waitForItemStatus(item.getId(), cn.aioi.problem.domain.BatchItemStatus.FAILED);

        assertThat(failed.getReanalysisProblemId()).isEqualTo(problem.getId());
        assertThat(failed.getErrorMessage()).contains("题目不存在");
        assertThat(problems.count()).isEqualTo(beforeCount);
    }

    @Test
    void reanalysisUsesCurrentProblemContentWhenProcessing() {
        User user = users.save(new User("batch-reanalyze-current", "hash"));
        Problem problem = problems.save(new Problem(
                "Current Content Reanalysis",
                "simple statement",
                DifficultyLevel.ENTRY,
                Set.of("旧标签"),
                "manual",
                user
        ));
        BatchJob job = jobs.save(new BatchJob("重新分析题目", user, 1));
        items.save(BatchJobItem.reanalysis(
                job,
                problem.getTitle(),
                problem.getDescription(),
                0,
                problem.getId()
        ));
        problem.update(
                problem.getTitle(),
                "dynamic programming dp 10^5",
                problem.getDifficulty(),
                problem.getTags(),
                problem.getSource()
        );
        problems.save(problem);

        batchJobService.triggerWorker();
        var updated = waitForProblemUpdate(problem.getId());

        assertThat(updated.getDescription()).isEqualTo("dynamic programming dp 10^5");
        assertThat(updated.getDifficulty().rank()).isGreaterThan(DifficultyLevel.ENTRY.rank());
    }

    private cn.aioi.problem.domain.Problem waitForProblem(String title) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            var match = problems.findAll().stream()
                    .filter(problem -> title.equals(problem.getTitle()))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Timed out waiting for normalized batch problem");
    }

    private Problem waitForProblemUpdate(Long id) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            Problem problem = problems.findById(id).orElseThrow();
            if (problem.getDifficulty().rank() > DifficultyLevel.ENTRY.rank()) {
                return problem;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Timed out waiting for reanalysis");
    }

    private BatchJobItem waitForItemStatus(Long id, cn.aioi.problem.domain.BatchItemStatus status) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            BatchJobItem item = items.findById(id).orElseThrow();
            if (item.getStatus() == status) {
                return item;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Timed out waiting for item status " + status);
    }
}
