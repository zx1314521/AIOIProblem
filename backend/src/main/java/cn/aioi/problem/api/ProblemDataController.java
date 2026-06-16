package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.ProblemDataService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems/{problemId}")
public class ProblemDataController {
    private final ProblemDataService problemDataService;

    public ProblemDataController(ProblemDataService problemDataService) {
        this.problemDataService = problemDataService;
    }

    @GetMapping("/data/status")
    ProblemDataDtos.DataStatusResponse status(@PathVariable Long problemId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return problemDataService.status(problemId, principal.user());
    }

    @GetMapping("/data")
    ProblemDataDtos.DataSetResponse detail(@PathVariable Long problemId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return problemDataService.detail(problemId, principal.user());
    }

    @PostMapping("/data/generate")
    ProblemDataDtos.DataStatusResponse generate(@PathVariable Long problemId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return problemDataService.startGeneration(problemId, principal.user());
    }

    @PostMapping("/data/cases")
    ProblemDataDtos.DataSetResponse addCase(@PathVariable Long problemId,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody ProblemDataDtos.DataCaseRequest request) {
        return problemDataService.addCase(problemId, principal.user(), request);
    }

    @PutMapping("/data/cases/{caseId}")
    ProblemDataDtos.DataSetResponse updateCase(@PathVariable Long problemId, @PathVariable Long caseId,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ProblemDataDtos.DataCaseRequest request) {
        return problemDataService.updateCase(problemId, principal.user(), caseId, request);
    }

    @DeleteMapping("/data/cases/{caseId}")
    ProblemDataDtos.DataSetResponse deleteCase(@PathVariable Long problemId, @PathVariable Long caseId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return problemDataService.deleteCase(problemId, principal.user(), caseId);
    }

    @PostMapping("/run/debug")
    ProblemDataDtos.CodeRunResponse runDebug(@PathVariable Long problemId,
                                             @AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody ProblemDataDtos.CodeRunRequest request) {
        return problemDataService.runDebug(problemId, principal.user(), request);
    }

    @PostMapping("/run/cases")
    ProblemDataDtos.CodeRunResponse runCases(@PathVariable Long problemId,
                                             @AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody ProblemDataDtos.CodeRunRequest request) {
        return problemDataService.runCases(problemId, principal.user(), request);
    }

    @GetMapping("/data/download")
    ResponseEntity<byte[]> download(@PathVariable Long problemId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        byte[] zip = problemDataService.zip(problemId, principal.user());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("problem-" + problemId + "-testdata.zip")
                        .build()
                        .toString())
                .body(zip);
    }
}
