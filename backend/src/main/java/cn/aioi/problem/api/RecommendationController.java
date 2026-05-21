package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.RecommendationDtos;
import cn.aioi.problem.security.UserPrincipal;
import cn.aioi.problem.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    RecommendationDtos.RecommendationResponse recommend(@AuthenticationPrincipal UserPrincipal principal) {
        return recommendationService.recommend(principal.user());
    }
}

