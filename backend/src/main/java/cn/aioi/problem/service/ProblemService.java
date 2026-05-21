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

import java.util.LinkedHashSet;
import java.util.List;
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
        String normalizedKeyword = blankToNull(keyword);
        String normalizedTag = blankToNull(tag);
        return problems.search(normalizedKeyword, parsedDifficulty, normalizedTag).stream()
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
}

