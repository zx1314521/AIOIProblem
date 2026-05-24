package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.TagDtos;
import cn.aioi.problem.service.TagCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TagCatalogController {
    private final TagCatalogService tagCatalog;

    public TagCatalogController(TagCatalogService tagCatalog) {
        this.tagCatalog = tagCatalog;
    }

    @GetMapping("/api/tags")
    TagDtos.TagCatalogResponse tags() {
        return new TagDtos.TagCatalogResponse(tagCatalog.categories());
    }
}
