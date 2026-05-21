package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AnalysisDtos;
import cn.aioi.problem.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/text")
    AnalysisDtos.AnalysisResponse analyzeText(@Valid @RequestBody AnalysisDtos.TextAnalysisRequest request) {
        return analysisService.analyzeText(request.title(), request.text());
    }

    @PostMapping("/file")
    AnalysisDtos.AnalysisResponse analyzeFile(@RequestParam("file") MultipartFile file) {
        return analysisService.analyzeFile(file);
    }
}

