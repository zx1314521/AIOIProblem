package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.ProblemSetDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.ProblemSetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problem-sets")
public class ProblemSetController {
    private final ProblemSetService problemSetService;

    public ProblemSetController(ProblemSetService problemSetService) {
        this.problemSetService = problemSetService;
    }

    @GetMapping
    List<ProblemSetDtos.ProblemSetResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.list(principal.user());
    }

    @PostMapping
    ProblemSetDtos.ProblemSetResponse create(@Valid @RequestBody ProblemSetDtos.ProblemSetRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.create(request, principal.user());
    }

    @PostMapping("/with-problems")
    ProblemSetDtos.ProblemSetResponse createWithProblems(@Valid @RequestBody ProblemSetDtos.ProblemSetWithProblemsRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.createWithProblems(request, principal.user());
    }

    @PostMapping("/{id}/items")
    ProblemSetDtos.ProblemSetResponse addProblem(@PathVariable Long id,
                                                 @Valid @RequestBody ProblemSetDtos.AddProblemRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.addProblem(id, request.problemId(), principal.user());
    }

    @PostMapping("/{id}/items/bulk")
    ProblemSetDtos.ProblemSetResponse addProblems(@PathVariable Long id,
                                                  @Valid @RequestBody ProblemSetDtos.AddProblemsRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.addProblems(id, request.problemIds(), principal.user());
    }

    @PostMapping("/{id}/items/reorder")
    ProblemSetDtos.ProblemSetResponse reorderProblems(@PathVariable Long id,
                                                      @Valid @RequestBody ProblemSetDtos.ReorderProblemsRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.reorderProblems(id, request.problemIds(), principal.user());
    }

    @DeleteMapping("/{id}/items/{problemId}")
    ProblemSetDtos.ProblemSetResponse removeProblem(@PathVariable Long id,
                                                    @PathVariable Long problemId,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return problemSetService.removeProblem(id, problemId, principal.user());
    }
}
