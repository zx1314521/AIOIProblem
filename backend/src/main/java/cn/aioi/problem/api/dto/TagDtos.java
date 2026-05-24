package cn.aioi.problem.api.dto;

import java.util.List;

public final class TagDtos {
    private TagDtos() {
    }

    public record TagCategory(String name, List<String> tags) {
    }

    public record TagCatalogResponse(List<TagCategory> categories) {
    }
}
