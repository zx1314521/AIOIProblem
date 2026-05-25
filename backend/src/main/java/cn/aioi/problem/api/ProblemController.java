package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
                                             @RequestParam(required = false) List<String> tags,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.search(keyword, difficulty, tag, tags, principal.user());
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

    @PutMapping("/{id}")
    ProblemDtos.ProblemResponse update(@PathVariable Long id,
                                       @Valid @RequestBody ProblemDtos.ProblemRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.update(id, request, principal.user());
    }

    @PostMapping("/{id}/passed")
    ProblemDtos.ProblemResponse markPassed(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.markPassed(id, principal.user());
    }

    @PostMapping("/bulk/passed")
    List<ProblemDtos.ProblemResponse> markPassedBulk(@Valid @RequestBody ProblemDtos.BulkProblemRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.markPassedBulk(request.problemIds(), principal.user());
    }

    @DeleteMapping("/{id}/passed")
    ProblemDtos.ProblemResponse unmarkPassed(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return problemService.unmarkPassed(id, principal.user());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        problemService.delete(id);
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBulk(@Valid @RequestBody ProblemDtos.BulkProblemRequest request) {
        problemService.deleteBulk(request.problemIds());
    }
}
