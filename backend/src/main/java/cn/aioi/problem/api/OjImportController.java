package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.OjImportDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.OjImportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/oj-imports")
public class OjImportController {
    private final OjImportService ojImportService;

    public OjImportController(OjImportService ojImportService) {
        this.ojImportService = ojImportService;
    }

    @PostMapping
    OjImportDtos.OjImportResponse importItems(@Valid @RequestBody OjImportDtos.OjImportRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ojImportService.importItems(request, principal.user());
    }

    @GetMapping("/history")
    List<OjImportDtos.OjImportHistoryJob> history(@AuthenticationPrincipal UserPrincipal principal) {
        return ojImportService.history(principal.user());
    }
}
