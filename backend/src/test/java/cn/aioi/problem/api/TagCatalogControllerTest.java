package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.TagDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TagCatalogControllerTest {
    @Autowired
    TestRestTemplate rest;

    @Test
    void returnsStandardTagCatalogWithoutAuthentication() {
        ResponseEntity<TagDtos.TagCatalogResponse> response = rest.getForEntity(
                "/api/tags",
                TagDtos.TagCatalogResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().categories())
                .anySatisfy(category -> {
                    assertThat(category.name()).isEqualTo("字符串");
                    assertThat(category.tags()).contains("KMP 算法", "字典树 Trie");
                });
    }
}
