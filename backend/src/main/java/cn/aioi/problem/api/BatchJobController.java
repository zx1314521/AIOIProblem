package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.BatchDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.BatchJobService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/batch-jobs")
public class BatchJobController {
    private final BatchJobService batchJobService;

    public BatchJobController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    @PostMapping
    BatchDtos.BatchJobDetailResponse upload(@RequestParam(required = false) String name,
                                            @RequestParam("files") MultipartFile[] files,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.upload(name, files, principal.user());
    }

    @GetMapping
    List<BatchDtos.BatchJobResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.list(principal.user());
    }

    @GetMapping("/{id}")
    BatchDtos.BatchJobDetailResponse detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.detail(id, principal.user());
    }

    @PostMapping("/{id}/pause")
    BatchDtos.BatchJobResponse pause(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.pause(id, principal.user());
    }

    @PostMapping("/{id}/resume")
    BatchDtos.BatchJobResponse resume(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.resume(id, principal.user());
    }

    @PatchMapping("/{id}/items/{itemId}")
    BatchDtos.BatchItemResponse updateItem(@PathVariable Long id,
                                           @PathVariable Long itemId,
                                           @Valid @RequestBody BatchDtos.BatchItemUpdateRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.updateItem(id, itemId, request, principal.user());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    BatchDtos.BatchJobDetailResponse deleteItem(@PathVariable Long id,
                                                @PathVariable Long itemId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.deleteItem(id, itemId, principal.user());
    }

    @PostMapping("/{id}/items/reorder")
    BatchDtos.BatchJobDetailResponse reorderItems(@PathVariable Long id,
                                                  @Valid @RequestBody BatchDtos.BatchItemReorderRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return batchJobService.reorderItems(id, request, principal.user());
    }
}
