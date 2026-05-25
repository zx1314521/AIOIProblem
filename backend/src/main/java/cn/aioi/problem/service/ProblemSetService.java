package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.api.dto.ProblemSetDtos;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.ProblemSet;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemSetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProblemSetService {
    private final ProblemSetRepository problemSets;
    private final ProblemService problemService;
    private final PassedProblemRepository passedProblems;

    public ProblemSetService(ProblemSetRepository problemSets, ProblemService problemService, PassedProblemRepository passedProblems) {
        this.problemSets = problemSets;
        this.problemService = problemService;
        this.passedProblems = passedProblems;
    }

    @Transactional(readOnly = true)
    public List<ProblemSetDtos.ProblemSetResponse> list(User user) {
        return problemSets.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(set -> toResponse(set, user))
                .toList();
    }

    @Transactional
    public ProblemSetDtos.ProblemSetResponse create(ProblemSetDtos.ProblemSetRequest request, User user) {
        ProblemSet set = problemSets.save(new ProblemSet(request.name().trim(), request.description(), user));
        return toResponse(set, user);
    }

    @Transactional
    public ProblemSetDtos.ProblemSetResponse createWithProblems(ProblemSetDtos.ProblemSetWithProblemsRequest request, User user) {
        ProblemSet set = problemSets.save(new ProblemSet(request.name().trim(), request.description(), user));
        addProblemIds(set, request.problemIds());
        return toResponse(set, user);
    }

    @Transactional
    public ProblemSetDtos.ProblemSetResponse addProblem(Long id, Long problemId, User user) {
        ProblemSet set = ownedSet(id, user);
        Problem problem = problemService.getProblem(problemId);
        set.addProblem(problem);
        return toResponse(set, user);
    }

    @Transactional
    public ProblemSetDtos.ProblemSetResponse addProblems(Long id, List<Long> problemIds, User user) {
        ProblemSet set = ownedSet(id, user);
        addProblemIds(set, problemIds);
        return toResponse(set, user);
    }

    @Transactional
    public ProblemSetDtos.ProblemSetResponse removeProblem(Long id, Long problemId, User user) {
        ProblemSet set = ownedSet(id, user);
        set.removeProblem(problemId);
        return toResponse(set, user);
    }

    private ProblemSet ownedSet(Long id, User user) {
        return problemSets.findByIdAndOwner(id, user).orElseThrow(() -> new EntityNotFoundException("题单不存在"));
    }

    private void addProblemIds(ProblemSet set, List<Long> problemIds) {
        problemIds.stream()
                .distinct()
                .map(problemService::getProblem)
                .forEach(set::addProblem);
    }

    private ProblemSetDtos.ProblemSetResponse toResponse(ProblemSet set, User user) {
        List<ProblemDtos.ProblemResponse> items = set.getItems().stream()
                .map(item -> ProblemDtos.ProblemResponse.from(
                        item.getProblem(),
                        passedProblems.existsByUserAndProblem(user, item.getProblem())))
                .toList();
        return new ProblemSetDtos.ProblemSetResponse(set.getId(), set.getName(), set.getDescription(), set.getCreatedAt(), items);
    }
}
