package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.ProblemSetItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProblemService {
    private final ProblemRepository problems;
    private final PassedProblemRepository passedProblems;
    private final ProblemSetItemRepository problemSetItems;
    private final BatchJobItemRepository batchJobItems;
    private final TagCatalogService tagCatalog;

    public ProblemService(ProblemRepository problems, PassedProblemRepository passedProblems,
                          ProblemSetItemRepository problemSetItems, BatchJobItemRepository batchJobItems,
                          TagCatalogService tagCatalog) {
        this.problems = problems;
        this.passedProblems = passedProblems;
        this.problemSetItems = problemSetItems;
        this.batchJobItems = batchJobItems;
        this.tagCatalog = tagCatalog;
    }

    public List<ProblemDtos.ProblemResponse> search(String keyword, String difficulty, String tag, List<String> tags, User user) {
        DifficultyLevel parsedDifficulty = difficulty == null || difficulty.isBlank()
                ? null
                : DifficultyLevel.fromLabelOrName(difficulty);
        String normalizedKeyword = lowerOrNull(keyword);
        List<String> normalizedTags = normalizeSearchTags(tag, tags);
        return problems.findAllWithTags().stream()
                .filter(problem -> matchesKeyword(problem, normalizedKeyword))
                .filter(problem -> parsedDifficulty == null || problem.getDifficulty() == parsedDifficulty)
                .sorted(searchComparator(normalizedTags))
                .map(problem -> ProblemDtos.ProblemResponse.from(problem, passedProblems.existsByUserAndProblem(user, problem)))
                .toList();
    }

    @Transactional
    public ProblemDtos.ProblemResponse create(ProblemDtos.ProblemRequest request, User user) {
        Set<String> tags = sanitizeTags(request.tags(), tagCatalog);
        Problem problem = problems.save(new Problem(
                ProblemTextNormalizer.normalizeNamesAndTrim(request.title()),
                ProblemTextNormalizer.normalizeNamesAndTrim(request.description()),
                DifficultyLevel.fromLabelOrName(request.difficulty()),
                tags,
                blankToNull(request.source()),
                user
        ));
        return ProblemDtos.ProblemResponse.from(problem, false);
    }

    @Transactional
    public ProblemDtos.ProblemResponse update(Long id, ProblemDtos.ProblemRequest request, User user) {
        Problem problem = getProblem(id);
        problem.update(
                ProblemTextNormalizer.normalizeNamesAndTrim(request.title()),
                ProblemTextNormalizer.normalizeNamesAndTrim(request.description()),
                DifficultyLevel.fromLabelOrName(request.difficulty()),
                sanitizeTags(request.tags(), tagCatalog),
                blankToNull(request.source())
        );
        return ProblemDtos.ProblemResponse.from(problem, passedProblems.existsByUserAndProblem(user, problem));
    }

    public Problem getProblem(Long id) {
        return problems.findById(id).orElseThrow(() -> new EntityNotFoundException("题目不存在"));
    }

    public ProblemDtos.ProblemResponse get(Long id, User user) {
        Problem problem = getProblem(id);
        return ProblemDtos.ProblemResponse.from(problem, passedProblems.existsByUserAndProblem(user, problem));
    }

    public List<ProblemDtos.DuplicateHint> similar(Long id, User user) {
        Problem target = getProblem(id);
        Set<String> targetTitleTokens = titleTokens(target.getTitle());
        Set<String> targetTags = comparableTags(target.getTags());
        return problems.findAllWithTags().stream()
                .filter(problem -> !problem.getId().equals(target.getId()))
                .map(problem -> duplicateHint(problem, targetTitleTokens, targetTags))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingInt(ProblemDtos.DuplicateHint::score).reversed()
                        .thenComparing(ProblemDtos.DuplicateHint::title))
                .limit(6)
                .toList();
    }

    @Transactional
    public ProblemDtos.ProblemResponse markPassed(Long id, User user) {
        Problem problem = getProblem(id);
        if (!passedProblems.existsByUserAndProblem(user, problem)) {
            passedProblems.save(new PassedProblem(user, problem));
        }
        return ProblemDtos.ProblemResponse.from(problem, true);
    }

    @Transactional
    public List<ProblemDtos.ProblemResponse> markPassedBulk(List<Long> ids, User user) {
        return distinctIds(ids).stream()
                .map(id -> markPassed(id, user))
                .toList();
    }

    @Transactional
    public ProblemDtos.ProblemResponse unmarkPassed(Long id, User user) {
        Problem problem = getProblem(id);
        passedProblems.findByUserAndProblem(user, problem).ifPresent(passedProblems::delete);
        return ProblemDtos.ProblemResponse.from(problem, false);
    }

    @Transactional
    public void delete(Long id) {
        Problem problem = getProblem(id);
        passedProblems.deleteByProblem(problem);
        problemSetItems.deleteByProblem(problem);
        batchJobItems.clearProblemReference(problem.getId());
        problems.delete(problem);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        distinctIds(ids).forEach(this::delete);
    }

    static Set<String> sanitizeTags(Set<String> tags, TagCatalogService tagCatalog) {
        if (tags == null || tags.isEmpty()) {
            return Set.of(TagCatalogService.NO_TAG);
        }
        Set<String> clean = new LinkedHashSet<>();
        List<String> invalid = new java.util.ArrayList<>();
        tags.stream()
                .forEach(tag -> {
                    if (tag == null || tag.trim().isBlank()) {
                        invalid.add("空标签");
                        return;
                    }
                    String trimmed = tag.trim();
                    if (tagCatalog.isStandardTag(trimmed)) {
                        clean.add(trimmed);
                    } else {
                        invalid.add(trimmed);
                    }
                });
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("未知标签：" + String.join("、", invalid));
        }
        Set<String> normalized = normalizeNoTagPlaceholder(clean);
        return normalized.stream().limit(12).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    static Set<String> sanitizeAiTags(List<String> tags, TagCatalogService tagCatalog) {
        return new LinkedHashSet<>(tagCatalog.normalizeAiTags(tags));
    }

    private static Set<String> normalizeNoTagPlaceholder(Set<String> tags) {
        if (tags.isEmpty()) {
            return Set.of(TagCatalogService.NO_TAG);
        }
        if (tags.size() > 1 && tags.contains(TagCatalogService.NO_TAG)) {
            LinkedHashSet<String> withoutPlaceholder = new LinkedHashSet<>(tags);
            withoutPlaceholder.remove(TagCatalogService.NO_TAG);
            return withoutPlaceholder;
        }
        return tags;
    }

    private List<String> normalizeSearchTags(String tag, List<String> tags) {
        List<String> values = new ArrayList<>();
        if (tags != null) {
            values.addAll(tags);
        }
        String legacyTag = blankToNull(tag);
        if (legacyTag != null) {
            values.add(legacyTag);
        }
        LinkedHashSet<String> clean = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = blankToNull(value);
            if (cleaned == null) {
                continue;
            }
            if (!tagCatalog.isStandardTag(cleaned)) {
                throw new IllegalArgumentException("未知标签：" + cleaned);
            }
            clean.add(cleaned);
        }
        return clean.stream().toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String lowerOrNull(String value) {
        String cleaned = blankToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private static List<Long> distinctIds(List<Long> ids) {
        return ids.stream()
                .distinct()
                .toList();
    }

    private boolean matchesKeyword(Problem problem, String keyword) {
        if (keyword == null) {
            return true;
        }
        return problem.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
                || problem.getDescription().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Comparator<Problem> searchComparator(List<String> selectedTags) {
        Comparator<Problem> recentFirst = Comparator.comparing(Problem::getCreatedAt).reversed();
        if (selectedTags.isEmpty()) {
            return recentFirst;
        }
        Set<String> relatedTags = selectedTags.stream()
                .flatMap(tag -> tagCatalog.relatedTags(tag).stream())
                .filter(tag -> !selectedTags.contains(tag))
                .collect(Collectors.toCollection(HashSet::new));
        return Comparator.comparingInt((Problem problem) -> relevanceScore(problem, selectedTags, relatedTags))
                .reversed()
                .thenComparing(recentFirst);
    }

    private int relevanceScore(Problem problem, List<String> selectedTags, Set<String> relatedTags) {
        int exact = (int) selectedTags.stream().filter(problem.getTags()::contains).count();
        int related = (int) problem.getTags().stream().filter(relatedTags::contains).count();
        return exact * 100 + related * 20;
    }

    private static ProblemDtos.DuplicateHint duplicateHint(Problem problem, Set<String> targetTitleTokens, Set<String> targetTags) {
        Set<String> candidateTitleTokens = titleTokens(problem.getTitle());
        List<String> sharedTitleTokens = candidateTitleTokens.stream()
                .filter(targetTitleTokens::contains)
                .sorted()
                .toList();
        Set<String> candidateTags = comparableTags(problem.getTags());
        List<String> sharedTags = candidateTags.stream()
                .filter(targetTags::contains)
                .sorted()
                .toList();
        int score = sharedTitleTokens.size() * 40 + sharedTags.size() * 30;
        List<String> reasons = new ArrayList<>();
        if (!sharedTitleTokens.isEmpty()) {
            reasons.add("title: " + String.join(", ", sharedTitleTokens));
        }
        if (!sharedTags.isEmpty()) {
            reasons.add("tag: " + String.join(", ", sharedTags));
        }
        return ProblemDtos.DuplicateHint.from(problem, score, String.join("; ", reasons));
    }

    private static Set<String> titleTokens(String title) {
        if (title == null || title.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(title.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> comparableTags(Set<String> tags) {
        return tags.stream()
                .filter(tag -> !TagCatalogService.NO_TAG.equals(tag))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
