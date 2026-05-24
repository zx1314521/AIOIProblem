package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.api.dto.ProblemDtos;
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
                "/api/problems?tag=最短路",
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
}
