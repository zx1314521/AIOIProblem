package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.api.dto.BatchDtos;
import cn.aioi.problem.api.dto.ProblemDataDtos;
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
class ProblemDataControllerTest {
    @Autowired
    TestRestTemplate rest;

    @Test
    void userCanManageProblemDataAndRunCode() {
        AuthDtos.AuthResponse auth = register("data-owner");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse problem = createProblem(headers);

        ResponseEntity<ProblemDataDtos.DataStatusResponse> emptyStatus = rest.exchange(
                "/api/problems/" + problem.id() + "/data/status",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDataDtos.DataStatusResponse.class
        );
        assertThat(emptyStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyStatus.getBody()).isNotNull();
        assertThat(emptyStatus.getBody().status()).isEqualTo("NONE");

        ProblemDataDtos.DataCaseRequest request = new ProblemDataDtos.DataCaseRequest(1, "2 3\n", "5\n");
        ResponseEntity<ProblemDataDtos.DataSetResponse> withCase = rest.exchange(
                "/api/problems/" + problem.id() + "/data/cases",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ProblemDataDtos.DataSetResponse.class
        );
        assertThat(withCase.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(withCase.getBody()).isNotNull();
        assertThat(withCase.getBody().status()).isEqualTo("READY");
        assertThat(withCase.getBody().cases()).hasSize(1);

        ProblemDataDtos.CodeRunRequest debugRequest = new ProblemDataDtos.CodeRunRequest(sumCode(), "4 7\n", null);
        ResponseEntity<ProblemDataDtos.CodeRunResponse> debug = rest.exchange(
                "/api/problems/" + problem.id() + "/run/debug",
                HttpMethod.POST,
                new HttpEntity<>(debugRequest, headers),
                ProblemDataDtos.CodeRunResponse.class
        );
        assertThat(debug.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(debug.getBody()).isNotNull();
        assertThat(debug.getBody().status()).isEqualTo("OK");
        assertThat(debug.getBody().stdout()).isEqualTo("11\n");

        ProblemDataDtos.CodeRunRequest runRequest = new ProblemDataDtos.CodeRunRequest(sumCode(), null, null);
        ResponseEntity<ProblemDataDtos.CodeRunResponse> run = rest.exchange(
                "/api/problems/" + problem.id() + "/run/cases",
                HttpMethod.POST,
                new HttpEntity<>(runRequest, headers),
                ProblemDataDtos.CodeRunResponse.class
        );
        assertThat(run.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(run.getBody()).isNotNull();
        assertThat(run.getBody().status()).isEqualTo("AC");
        assertThat(run.getBody().cases()).extracting(ProblemDataDtos.CaseRunResponse::status).containsExactly("AC");
    }

    @Test
    void authenticatedUserCanAccessVisibleProblemDataCreatedByAnotherUser() {
        AuthDtos.AuthResponse owner = register("data-owner-private");
        ProblemDtos.ProblemResponse problem = createProblem(bearer(owner.token()));
        AuthDtos.AuthResponse other = register("data-other");

        ResponseEntity<ProblemDataDtos.DataStatusResponse> response = rest.exchange(
                "/api/problems/" + problem.id() + "/data/status",
                HttpMethod.GET,
                new HttpEntity<>(bearer(other.token())),
                ProblemDataDtos.DataStatusResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("NONE");

        ResponseEntity<ProblemDataDtos.DataStatusResponse> generate = rest.exchange(
                "/api/problems/" + problem.id() + "/data/generate",
                HttpMethod.POST,
                new HttpEntity<>(bearer(other.token())),
                ProblemDataDtos.DataStatusResponse.class
        );

        assertThat(generate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(generate.getBody()).isNotNull();
        assertThat(generate.getBody().status()).isEqualTo("GENERATING");
    }

    @Test
    void authenticatedUserCanStartAiDataGeneration() {
        AuthDtos.AuthResponse auth = register("data-generator");
        HttpHeaders headers = bearer(auth.token());
        ProblemDtos.ProblemResponse problem = createProblem(headers);

        ResponseEntity<ProblemDataDtos.DataStatusResponse> response = rest.exchange(
                "/api/problems/" + problem.id() + "/data/generate",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ProblemDataDtos.DataStatusResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("GENERATING");

        ResponseEntity<BatchDtos.BatchJobResponse[]> jobs = rest.exchange(
                "/api/batch-jobs",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                BatchDtos.BatchJobResponse[].class
        );
        assertThat(jobs.getBody()).isNotNull();
        assertThat(jobs.getBody()).hasSize(1);

        ResponseEntity<BatchDtos.BatchJobDetailResponse> detail = rest.exchange(
                "/api/batch-jobs/" + jobs.getBody()[0].id(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                BatchDtos.BatchJobDetailResponse.class
        );
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().items()).hasSize(1);
        assertThat(detail.getBody().items().get(0).taskType()).isEqualTo("DATA_GENERATION");
    }

    private ProblemDtos.ProblemResponse createProblem(HttpHeaders headers) {
        ProblemDtos.ProblemRequest request = new ProblemDtos.ProblemRequest(
                "A+B",
                "Read two integers and output their sum.",
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
        return created.getBody();
    }

    private String sumCode() {
        return """
                #include <bits/stdc++.h>
                using namespace std;
                int main() {
                    long long a, b;
                    cin >> a >> b;
                    cout << a + b << "\\n";
                }
                """;
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
