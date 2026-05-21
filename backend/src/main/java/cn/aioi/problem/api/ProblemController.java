package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {
    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    List<ProblemDtos.ProblemResponse> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String difficulty,
                                             @RequestParam(required = false) String tag,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.search(keyword, difficulty, tag, principal.user());
    }

    @PostMapping
    ProblemDtos.ProblemResponse create(@Valid @RequestBody ProblemDtos.ProblemRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.create(request, principal.user());
    }

    @GetMapping("/{id}")
    ProblemDtos.ProblemResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.get(id, principal.user());
    }

    @PostMapping("/{id}/passed")
    ProblemDtos.ProblemResponse markPassed(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.markPassed(id, principal.user());
    }
}

