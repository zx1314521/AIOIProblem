package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.api.dto.ProblemDtos;
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
                Set.of("图论", "最短路"),
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
                "/api/problems?tag=图论",
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

