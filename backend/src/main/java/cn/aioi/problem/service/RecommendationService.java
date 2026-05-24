package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.api.dto.RecommendationDtos;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final ProblemRepository problems;
    private final PassedProblemRepository passedProblems;

    public RecommendationService(ProblemRepository problems, PassedProblemRepository passedProblems) {
        this.problems = problems;
        this.passedProblems = passedProblems;
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.RecommendationResponse recommend(User user) {
        List<PassedProblem> passed = passedProblems.findByUser(user);
        Set<Long> passedIds = passed.stream().map(item -> item.getProblem().getId()).collect(Collectors.toSet());
        Map<String, Integer> solvedTagCounts = tagCounts(passed.stream().map(PassedProblem::getProblem).toList());
        List<Problem> candidates = problems.findAllWithTags().stream()
                .filter(problem -> !passedIds.contains(problem.getId()))
                .toList();

        List<String> weakTags = weakTags(candidates, solvedTagCounts);
        Set<String> weakSet = new HashSet<>(weakTags);
        int targetRank = targetRank(passed);

        List<RecommendationDtos.RecommendationItem> items = candidates.stream()
                .map(problem -> new ScoredRecommendation(problem, score(problem, weakSet, targetRank)))
                .sorted(Comparator.comparingInt(ScoredRecommendation::score).reversed()
                        .thenComparing(scored -> scored.problem.getDifficulty().rank()))
                .limit(8)
                .map(scored -> new RecommendationDtos.RecommendationItem(
                        ProblemDtos.ProblemResponse.from(scored.problem, false),
                        reason(scored.problem, weakSet, targetRank),
                        0
                ))
                .toList();

        List<RecommendationDtos.RecommendationItem> ordered = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            RecommendationDtos.RecommendationItem item = items.get(i);
            ordered.add(new RecommendationDtos.RecommendationItem(item.problem(), item.reason(), i + 1));
        }
        return new RecommendationDtos.RecommendationResponse(weakTags, ordered);
    }

    private Map<String, Integer> tagCounts(List<Problem> solved) {
        Map<String, Integer> counts = new HashMap<>();
        solved.forEach(problem -> problem.getTags().forEach(tag -> counts.merge(tag, 1, Integer::sum)));
        return counts;
    }

    private List<String> weakTags(List<Problem> candidates, Map<String, Integer> solvedTagCounts) {
        Map<String, Integer> available = tagCounts(candidates);
        if (available.isEmpty()) {
            return List.of();
        }
        List<String> weak = available.keySet().stream()
                .sorted(Comparator.comparingInt((String tag) -> solvedTagCounts.getOrDefault(tag, 0))
                        .thenComparing(available::get, Comparator.reverseOrder()))
                .limit(5)
                .toList();
        return weak.isEmpty() ? available.keySet().stream().limit(5).toList() : weak;
    }

    private int targetRank(List<PassedProblem> passed) {
        if (passed.isEmpty()) {
            return 2;
        }
        double average = passed.stream().mapToInt(item -> item.getProblem().getDifficulty().rank()).average().orElse(2);
        return Math.min(6, (int) Math.ceil(average) + 1);
    }

    private int score(Problem problem, Set<String> weakTags, int targetRank) {
        int weakScore = (int) problem.getTags().stream().filter(weakTags::contains).count() * 10;
        int difficultyFit = Math.max(0, 8 - Math.abs(problem.getDifficulty().rank() - targetRank) * 2);
        return weakScore + difficultyFit;
    }

    private String reason(Problem problem, Set<String> weakTags, int targetRank) {
        String matched = problem.getTags().stream().filter(weakTags::contains).collect(Collectors.joining("、"));
        String tagReason = matched.isBlank() ? "覆盖新的训练标签" : "补强薄弱标签：" + matched;
        String levelReason = problem.getDifficulty().rank() <= targetRank ? "难度适合作为当前阶段练习" : "难度略高，适合挑战";
        return tagReason + "；" + levelReason + "。";
    }

    private record ScoredRecommendation(Problem problem, int score) {
    }
}
