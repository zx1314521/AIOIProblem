package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.api.dto.BatchDtos;
import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.api.dto.ProblemSetDtos;
import cn.aioi.problem.api.dto.RecommendationDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthAndProblemIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Test
    void userCanRegisterCreateSearchAndMarkProblemPassed() {
        AuthDtos.AuthResponse auth = register("alice");
        HttpHeaders headers = bearer(auth.token());

        ProblemDtos.ProblemRequest request = new ProblemDtos.ProblemRequest(
                "最短路入门",
                "给定一张图，求从 1 到 n 的最短路。",
                "CSPJ中等",
                Set.of("最短路", "图遍历"),
                "internal"
        );

        ResponseEntity<ProblemDtos.ProblemResponse> created = rest.exchange(
                "/api/problems",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().difficulty()).isEqualTo("CSPJ中等");

        ResponseEntity<ProblemDtos.ProblemResponse[]> searched = rest.exchange(
                "/api/problems?keyword=最短路入门&tag=最短路",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse[].class
        );
        assertThat(searched.getBody()).hasSize(1);

        ResponseEntity<ProblemDtos.ProblemResponse> passed = rest.exchange(
                "/api/problems/" + created.getBody().id() + "/passed",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(passed.getBody()).isNotNull();
        assertThat(passed.getBody().passed()).isTrue();

        ResponseEntity<RecommendationDtos.RecommendationResponse> recommendations = rest.exchange(
                "/api/recommendations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationDtos.RecommendationResponse.class
        );
        assertThat(recommendations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recommendations.getBody()).isNotNull();

        ResponseEntity<ProblemDtos.ProblemResponse> unpassed = rest.exchange(
                "/api/problems/" + created.getBody().id() + "/passed",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(unpassed.getBody()).isNotNull();
        assertThat(unpassed.getBody().passed()).isFalse();

        passed = rest.exchange(
                "/api/problems/" + created.getBody().id() + "/passed",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(passed.getBody()).isNotNull();
        assertThat(passed.getBody().passed()).isTrue();

        ProblemDtos.ProblemRequest update = new ProblemDtos.ProblemRequest(
                "最短路提高",
                "更新后的题面。",
                "CSPS提高",
                Set.of("最短路", "网络流"),
                "internal"
        );
        ResponseEntity<ProblemDtos.ProblemResponse> updated = rest.exchange(
                "/api/problems/" + created.getBody().id(),
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().title()).isEqualTo("最短路提高");
        assertThat(updated.getBody().passed()).isTrue();

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/problems/" + created.getBody().id(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rejectsManualCategoryAndUnknownTags() {
        AuthDtos.AuthResponse auth = register("bob");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemRequest request = new ProblemDtos.ProblemRequest(
                "非法标签",
                "题面",
                "简单",
                Set.of("图论", "未知标签"),
                "internal"
        );

        ResponseEntity<String> response = rest.exchange(
                "/api/problems",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("未知标签");
    }

    @Test
    void normalizesWowoNameWhenCreatingProblems() {
        AuthDtos.AuthResponse auth = register("wowo-normalizer");
        HttpHeaders headers = bearer(auth.token());

        ProblemDtos.ProblemRequest request = new ProblemDtos.ProblemRequest(
                "\u8717\u8717 的训练题",
                "请帮助 \u8717\u8717 输出答案。",
                "简单",
                Set.of("模拟"),
                "manual"
        );

        ResponseEntity<ProblemDtos.ProblemResponse> created = rest.exchange(
                "/api/problems",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ProblemDtos.ProblemResponse.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().title()).isEqualTo("BOB 的训练题");
        assertThat(created.getBody().description()).isEqualTo("请帮助 BOB 输出答案。");
    }

    @Test
    void multiTagSearchRanksExactMatchesBeforePartialAndRelatedResults() {
        AuthDtos.AuthResponse auth = register("carol");
        HttpHeaders headers = bearer(auth.token());
        createProblem(headers, "多标签排序 A", Set.of("最短路", "网络流"));
        createProblem(headers, "多标签排序 B", Set.of("最短路"));
        createProblem(headers, "多标签排序 C", Set.of("图遍历"));
        createProblem(headers, "多标签排序 D", Set.of("模拟"));

        ResponseEntity<ProblemDtos.ProblemResponse[]> searched = rest.exchange(
                "/api/problems?keyword=多标签排序&tags=最短路&tags=网络流",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse[].class
        );

        assertThat(searched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searched.getBody()).isNotNull();
        assertThat(java.util.Arrays.stream(searched.getBody()).map(ProblemDtos.ProblemResponse::title).toList())
                .containsExactly("多标签排序 A", "多标签排序 B", "多标签排序 C", "多标签排序 D");
    }

    @Test
    void listsSimilarProblemHintsForMergeReview() {
        AuthDtos.AuthResponse auth = register("merge-review");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse target = createProblem(headers, "CF2229D Median Splits", Set.of());
        ProblemDtos.ProblemResponse sameTopic = createProblem(headers, "Median Splits practice", Set.of());
        createProblem(headers, "Unrelated Graph", Set.of());

        ResponseEntity<ProblemDtos.DuplicateHint[]> hints = rest.exchange(
                "/api/problems/" + target.id() + "/similar",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDtos.DuplicateHint[].class
        );

        assertThat(hints.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hints.getBody()).isNotNull();
        assertThat(hints.getBody()).extracting(ProblemDtos.DuplicateHint::id)
                .contains(sameTopic.id())
                .doesNotContain(target.id());
        assertThat(hints.getBody()[0].score()).isGreaterThan(0);
        assertThat(hints.getBody()[0].reason()).contains("title");
    }

    @Test
    void userCanBulkMarkPassedDeleteAndAddProblemsToSets() {
        AuthDtos.AuthResponse auth = register("diana");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse first = createProblem(headers, "批量操作 A", Set.of("模拟"));
        ProblemDtos.ProblemResponse second = createProblem(headers, "批量操作 B", Set.of("贪心"));

        ProblemDtos.BulkProblemRequest bulkRequest = new ProblemDtos.BulkProblemRequest(List.of(first.id(), second.id()));
        ResponseEntity<ProblemDtos.ProblemResponse[]> passed = rest.exchange(
                "/api/problems/bulk/passed",
                HttpMethod.POST,
                new HttpEntity<>(bulkRequest, headers),
                ProblemDtos.ProblemResponse[].class
        );

        assertThat(passed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(passed.getBody()).isNotNull();
        assertThat(passed.getBody()).extracting(ProblemDtos.ProblemResponse::passed).containsOnly(true);

        ProblemSetDtos.ProblemSetWithProblemsRequest setRequest = new ProblemSetDtos.ProblemSetWithProblemsRequest(
                "批量练习",
                "从题目管理批量加入",
                List.of(first.id(), second.id())
        );
        ResponseEntity<ProblemSetDtos.ProblemSetResponse> createdSet = rest.exchange(
                "/api/problem-sets/with-problems",
                HttpMethod.POST,
                new HttpEntity<>(setRequest, headers),
                ProblemSetDtos.ProblemSetResponse.class
        );
        assertThat(createdSet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdSet.getBody()).isNotNull();
        assertThat(createdSet.getBody().problems()).extracting(ProblemDtos.ProblemResponse::title)
                .containsExactlyInAnyOrder("批量操作 A", "批量操作 B");

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/problems/bulk",
                HttpMethod.DELETE,
                new HttpEntity<>(bulkRequest, headers),
                Void.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ProblemDtos.ProblemResponse[]> searched = rest.exchange(
                "/api/problems?keyword=批量操作",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse[].class
        );
        assertThat(searched.getBody()).isEmpty();
    }

    @Test
    void userCanQueueSelectedProblemsForReanalysis() {
        AuthDtos.AuthResponse auth = register("reanalyze-api");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse first = createProblem(headers, "重新分析 A", Set.of());
        ProblemDtos.ProblemResponse second = createProblem(headers, "重新分析 B", Set.of());

        ProblemDtos.BulkProblemRequest request = new ProblemDtos.BulkProblemRequest(List.of(first.id(), second.id()));
        ResponseEntity<BatchDtos.BatchJobDetailResponse> queued = rest.exchange(
                "/api/problems/reanalyze",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                BatchDtos.BatchJobDetailResponse.class
        );

        assertThat(queued.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queued.getBody()).isNotNull();
        assertThat(queued.getBody().job().name()).isEqualTo("重新分析题目");
        assertThat(queued.getBody().job().totalCount()).isEqualTo(2);
        assertThat(queued.getBody().items()).extracting(BatchDtos.BatchItemResponse::problemId)
                .containsExactly(first.id(), second.id());
    }

    @Test
    void rejectsNullIdsInBulkRequests() {
        AuthDtos.AuthResponse auth = register("eric");
        HttpHeaders headers = bearer(auth.token());

        ResponseEntity<String> response = rest.exchange(
                "/api/problems/bulk/passed",
                HttpMethod.POST,
                new HttpEntity<>("{\"problemIds\":[null]}", jsonHeaders(headers)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("problemIds");
    }

    @Test
    void userCanReorderProblemSetItems() {
        AuthDtos.AuthResponse auth = register("fiona");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse first = createProblem(headers, "题单排序 A", Set.of("模拟"));
        ProblemDtos.ProblemResponse second = createProblem(headers, "题单排序 B", Set.of("贪心"));
        ProblemDtos.ProblemResponse third = createProblem(headers, "题单排序 C", Set.of("最短路"));

        ProblemSetDtos.ProblemSetWithProblemsRequest setRequest = new ProblemSetDtos.ProblemSetWithProblemsRequest(
                "排序训练",
                "调整练习顺序",
                List.of(first.id(), second.id(), third.id())
        );
        ResponseEntity<ProblemSetDtos.ProblemSetResponse> createdSet = rest.exchange(
                "/api/problem-sets/with-problems",
                HttpMethod.POST,
                new HttpEntity<>(setRequest, headers),
                ProblemSetDtos.ProblemSetResponse.class
        );
        assertThat(createdSet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdSet.getBody()).isNotNull();

        ProblemSetDtos.ReorderProblemsRequest reorderRequest = new ProblemSetDtos.ReorderProblemsRequest(
                List.of(third.id(), first.id(), second.id())
        );
        ResponseEntity<ProblemSetDtos.ProblemSetResponse> reordered = rest.exchange(
                "/api/problem-sets/" + createdSet.getBody().id() + "/items/reorder",
                HttpMethod.POST,
                new HttpEntity<>(reorderRequest, headers),
                ProblemSetDtos.ProblemSetResponse.class
        );

        assertThat(reordered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reordered.getBody()).isNotNull();
        assertThat(reordered.getBody().problems()).extracting(ProblemDtos.ProblemResponse::title)
                .containsExactly("题单排序 C", "题单排序 A", "题单排序 B");
    }

    private ProblemDtos.ProblemResponse createProblem(HttpHeaders headers, String title, Set<String> tags) {
        ProblemDtos.ProblemRequest request = new ProblemDtos.ProblemRequest(
                title,
                "用于搜索排序的题面。",
                "简单",
                tags,
                "test"
        );
        ResponseEntity<ProblemDtos.ProblemResponse> created = rest.exchange(
                "/api/problems",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        return created.getBody();
    }

    private AuthDtos.AuthResponse register(String username) {
        ResponseEntity<AuthDtos.AuthResponse> response = rest.postForEntity(
                "/api/auth/register",
                new AuthDtos.RegisterRequest(username, "password123"),
                AuthDtos.AuthResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders jsonHeaders(HttpHeaders source) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(source);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
