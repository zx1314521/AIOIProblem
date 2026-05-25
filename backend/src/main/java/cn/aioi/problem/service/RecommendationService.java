package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.api.dto.RecommendationDtos;
import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.ai.ProblemInput;
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
    private final AiProvider aiProvider;

    public RecommendationService(ProblemRepository problems, PassedProblemRepository passedProblems, AiProvider aiProvider) {
        this.problems = problems;
        this.passedProblems = passedProblems;
        this.aiProvider = aiProvider;
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.RecommendationResponse recommend(User user) {
        List<PassedProblem> passed = passedProblems.findByUser(user);
        Set<Long> passedIds = passed.stream().map(item -> item.getProblem().getId()).collect(Collectors.toSet());
        Map<String, Integer> solvedTagCounts = tagCounts(passed.stream().map(PassedProblem::getProblem).toList());
        List<Problem> candidates = problems.findAllWithTags().stream()
                .filter(problem -> !passedIds.contains(problem.getId()))
                .toList();

        RecommendationPlan aiPlan = aiPlan(passed, candidates);
        List<String> weakTags = mergeWeakTags(aiPlan.tags(), weakTags(candidates, solvedTagCounts), candidates);
        Set<String> weakSet = new HashSet<>(weakTags);
        int targetRank = aiPlan.targetRank() == null ? targetRank(passed) : aiPlan.targetRank();

        List<RecommendationDtos.RecommendationItem> items = candidates.stream()
                .map(problem -> new ScoredRecommendation(problem, score(problem, weakTags, targetRank)))
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

    private RecommendationPlan aiPlan(List<PassedProblem> passed, List<Problem> candidates) {
        if (candidates.isEmpty()) {
            return new RecommendationPlan(List.of(), null);
        }
        try {
            AiAssessment assessment = aiProvider.assess(new ProblemInput("AI推荐", recommendationPrompt(passed, candidates)), AiTaskType.RECOMMENDATION);
            return new RecommendationPlan(assessment.tags(), assessment.difficulty().rank());
        } catch (RuntimeException ignored) {
            return new RecommendationPlan(List.of(), null);
        }
    }

    private String recommendationPrompt(List<PassedProblem> passed, List<Problem> candidates) {
        String solved = passed.stream()
                .map(item -> problemBrief(item.getProblem()))
                .limit(40)
                .collect(Collectors.joining("\n"));
        String available = candidates.stream()
                .map(this::problemBrief)
                .limit(80)
                .collect(Collectors.joining("\n"));
        return """
                根据学生已通过题目和候选题库，判断下一阶段应补强的算法标签和目标难度。
                只需要输出符合系统 JSON schema 的 difficulty、tags、hints、reasoningSummary。
                已通过：
                """ + (solved.isBlank() ? "暂无" : solved) + "\n候选题：\n" + available;
    }

    private String problemBrief(Problem problem) {
        return problem.getTitle() + " / " + problem.getDifficulty().label() + " / " + String.join("、", problem.getTags());
    }

    private List<String> mergeWeakTags(List<String> aiTags, List<String> ruleTags, List<Problem> candidates) {
        Set<String> available = candidates.stream()
                .flatMap(problem -> problem.getTags().stream())
                .collect(Collectors.toSet());
        List<String> merged = new java.util.ArrayList<>();
        aiTags.stream()
                .filter(available::contains)
                .forEach(tag -> addUnique(merged, tag));
        ruleTags.forEach(tag -> addUnique(merged, tag));
        return merged.stream().limit(5).toList();
    }

    private void addUnique(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private int score(Problem problem, List<String> weakTags, int targetRank) {
        int weakScore = 0;
        for (int index = 0; index < weakTags.size(); index++) {
            if (problem.getTags().contains(weakTags.get(index))) {
                weakScore += (weakTags.size() - index) * 10;
            }
        }
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

    private record RecommendationPlan(List<String> tags, Integer targetRank) {
    }
}
