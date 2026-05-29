package cn.aioi.problem.service;

import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.BatchJobRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BatchJobServiceTest {
    @Autowired
    BatchJobService batchJobService;

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
}
