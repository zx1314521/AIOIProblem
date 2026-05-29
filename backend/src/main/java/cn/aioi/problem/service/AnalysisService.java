package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.ai.ProblemInput;
import cn.aioi.problem.api.dto.AnalysisDtos;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    private final AiProvider aiProvider;
    private final ProblemRepository problems;

    public AnalysisService(AiProvider aiProvider, ProblemRepository problems) {
        this.aiProvider = aiProvider;
        this.problems = problems;
    }

    public AnalysisDtos.AnalysisResponse analyzeText(String title, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("题面不能为空");
        }
        String safeTitle = title == null || title.isBlank() ? "未命名题目" : title.trim();
        safeTitle = ProblemTextNormalizer.normalizeNamesAndTrim(safeTitle);
        ProblemInput rawInput = new ProblemInput(safeTitle, ProblemTextNormalizer.normalizeNamesAndTrim(text));
        AiAssessment assessment = aiProvider.assess(contentForAnalysis(rawInput), AiTaskType.PROBLEM_ANALYSIS);
        return toResponse(assessment);
    }

    public AnalysisDtos.AnalysisResponse analyzeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".txt") && !filename.endsWith(".md")) {
            throw new IllegalArgumentException("仅支持 .txt 或 .md 文件");
        }
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return analyzeText(file.getOriginalFilename(), text);
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件读取失败");
        }
    }

    private AnalysisDtos.AnalysisResponse toResponse(AiAssessment assessment) {
        return new AnalysisDtos.AnalysisResponse(
                assessment.difficulty().label(),
                assessment.difficulty().name(),
                assessment.confidence(),
                assessment.tags(),
                normalizedHints(assessment.hints()),
                assessment.reasoningSummary(),
                similarProblems(assessment.tags())
        );
    }

    private ProblemInput contentForAnalysis(ProblemInput rawInput) {
        try {
            String polished = aiProvider.polishProblemStatement(rawInput, AiTaskType.PROBLEM_ANALYSIS);
            if (polished == null || polished.isBlank()) {
                return rawInput;
            }
            return new ProblemInput(rawInput.title(), ProblemTextNormalizer.normalizeNamesAndTrim(polished));
        } catch (RuntimeException exception) {
            return rawInput;
        }
    }

    private List<String> normalizedHints(List<String> hints) {
        if (hints.size() >= 3) {
            return hints;
        }
        return List.of(
                hints.isEmpty() ? "先读清楚输入规模和目标输出。" : hints.get(0),
                hints.size() > 1 ? hints.get(1) : "尝试把题目归类到常见算法模型。",
                "最后考虑复杂度瓶颈，并选择合适的数据结构或状态设计。"
        );
    }

    private List<AnalysisDtos.SimilarProblem> similarProblems(List<String> tags) {
        Set<String> tagSet = tags.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return problems.findAllWithTags().stream()
                .map(problem -> new ScoredProblem(problem, overlap(problem, tagSet)))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingInt(ScoredProblem::score).reversed())
                .limit(5)
                .map(scored -> new AnalysisDtos.SimilarProblem(
                        scored.problem.getId(),
                        scored.problem.getTitle(),
                        scored.problem.getDifficulty().label(),
                        scored.problem.getTags().stream().sorted().toList(),
                        "标签相似：" + scored.problem.getTags().stream().filter(tag -> tagSet.contains(tag.toLowerCase())).collect(Collectors.joining("、"))
                ))
                .toList();
    }

    private int overlap(Problem problem, Set<String> tags) {
        return (int) problem.getTags().stream().filter(tag -> tags.contains(tag.toLowerCase())).count();
    }

    private record ScoredProblem(Problem problem, int score) {
    }
}
