package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProblemService {
    private final ProblemRepository problems;
    private final PassedProblemRepository passedProblems;

    public ProblemService(ProblemRepository problems, PassedProblemRepository passedProblems) {
        this.problems = problems;
        this.passedProblems = passedProblems;
    }

    public List<ProblemDtos.ProblemResponse> search(String keyword, String difficulty, String tag, User user) {
        DifficultyLevel parsedDifficulty = difficulty == null || difficulty.isBlank()
                ? null
                : DifficultyLevel.fromLabelOrName(difficulty);
        String normalizedKeyword = lowerOrNull(keyword);
        String normalizedTag = lowerOrNull(tag);
        return problems.findAllWithTags().stream()
                .filter(problem -> matchesKeyword(problem, normalizedKeyword))
                .filter(problem -> parsedDifficulty == null || problem.getDifficulty() == parsedDifficulty)
                .filter(problem -> matchesTag(problem, normalizedTag))
                .sorted(Comparator.comparing(Problem::getCreatedAt).reversed())
                .map(problem -> ProblemDtos.ProblemResponse.from(problem, passedProblems.existsByUserAndProblem(user, problem)))
                .toList();
    }

    @Transactional
    public ProblemDtos.ProblemResponse create(ProblemDtos.ProblemRequest request, User user) {
        Set<String> tags = sanitizeTags(request.tags());
        Problem problem = problems.save(new Problem(
                request.title().trim(),
                request.description().trim(),
                DifficultyLevel.fromLabelOrName(request.difficulty()),
                tags,
                blankToNull(request.source()),
                user
        ));
        return ProblemDtos.ProblemResponse.from(problem, false);
    }

    public Problem getProblem(Long id) {
        return problems.findById(id).orElseThrow(() -> new EntityNotFoundException("题目不存在"));
    }

    public ProblemDtos.ProblemResponse get(Long id, User user) {
        Problem problem = getProblem(id);
        return ProblemDtos.ProblemResponse.from(problem, passedProblems.existsByUserAndProblem(user, problem));
    }

    @Transactional
    public ProblemDtos.ProblemResponse markPassed(Long id, User user) {
        Problem problem = getProblem(id);
        if (!passedProblems.existsByUserAndProblem(user, problem)) {
            passedProblems.save(new PassedProblem(user, problem));
        }
        return ProblemDtos.ProblemResponse.from(problem, true);
    }

    static Set<String> sanitizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        Set<String> clean = new LinkedHashSet<>();
        tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .limit(12)
                .forEach(clean::add);
        return clean;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String lowerOrNull(String value) {
        String cleaned = blankToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(Problem problem, String keyword) {
        if (keyword == null) {
            return true;
        }
        return problem.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
                || problem.getDescription().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesTag(Problem problem, String tag) {
        if (tag == null) {
            return true;
        }
        return problem.getTags().stream().anyMatch(item -> item.toLowerCase(Locale.ROOT).equals(tag));
    }
}
