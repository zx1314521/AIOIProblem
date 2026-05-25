package cn.aioi.problem.service;

import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.repository.ProblemRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ProblemTagCleanupService {
    private final ProblemRepository problems;
    private final TagCatalogService tagCatalog;

    public ProblemTagCleanupService(ProblemRepository problems, TagCatalogService tagCatalog) {
        this.problems = problems;
        this.tagCatalog = tagCatalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void normalizeExistingProblemTags() {
        problems.findAllWithTags().forEach(this::normalizeProblemTags);
    }

    void normalizeProblemTags(Problem problem) {
        Set<String> normalized = new LinkedHashSet<>(tagCatalog.normalizeTags(problem.getTags().stream().toList()));
        if (normalized.isEmpty()) {
            normalized.add(TagCatalogService.NO_TAG);
        }
        if (!normalized.equals(problem.getTags())) {
            problem.update(
                    problem.getTitle(),
                    problem.getDescription(),
                    problem.getDifficulty(),
                    normalized,
                    problem.getSource()
            );
        }
    }
}
