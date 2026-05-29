package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.api.dto.OjImportDtos;
import cn.aioi.problem.api.dto.ProblemDtos;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.repository.ProblemRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OjImportControllerTest {
    @Autowired
    TestRestTemplate rest;

    @Autowired
    ProblemRepository problems;

    @Test
    void rejectsAnonymousImportRequests() {
        OjImportDtos.OjImportRequest request = new OjImportDtos.OjImportRequest(List.of(validItem("CF2229D", true)));

        ResponseEntity<String> response = rest.postForEntity("/api/oj-imports", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidHistoryTokenAsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");

        ResponseEntity<String> response = rest.exchange(
                "/api/oj-imports/history",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void queuesAcceptedProblemAndMarksItPassedAfterAnalysis() {
        AuthDtos.AuthResponse auth = register("ojalice");
        HttpHeaders headers = bearer(auth.token());

        ResponseEntity<OjImportDtos.OjImportResponse> imported = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(validItem("CF2229D", true))), headers),
                OjImportDtos.OjImportResponse.class
        );

        assertThat(imported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(imported.getBody()).isNotNull();
        assertThat(imported.getBody().items()).hasSize(1);
        assertThat(imported.getBody().items().getFirst().status()).isEqualTo("QUEUED");

        ProblemDtos.ProblemResponse problem = waitForProblem(headers, "CODEFORCES", "CF2229D");
        assertThat(problem.source()).isEqualTo("Codeforces: CF2229D");
        assertThat(problem.passed()).isTrue();
        assertThat(problem.description()).doesNotContain("\n\n\n");
    }

    @Test
    void doesNotCreateDuplicateProblemForExistingSourceId() {
        AuthDtos.AuthResponse auth = register("ojbob");
        HttpHeaders headers = bearer(auth.token());

        importOne(headers, validItem("CF2229E", false));
        ProblemDtos.ProblemResponse first = waitForProblem(headers, "CODEFORCES", "CF2229E");

        ResponseEntity<OjImportDtos.OjImportResponse> duplicate = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(validItem("CF2229E", true))), headers),
                OjImportDtos.OjImportResponse.class
        );

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicate.getBody()).isNotNull();
        assertThat(duplicate.getBody().items().getFirst().status()).isEqualTo("EXISTS_MARKED_PASSED");

        List<Problem> matchingSource = problems.findAll().stream()
                .filter(problem -> "CODEFORCES".equals(problem.getExternalPlatform()))
                .filter(problem -> "CF2229E".equals(problem.getExternalSourceId()))
                .toList();
        assertThat(matchingSource).hasSize(1);

        ResponseEntity<ProblemDtos.ProblemResponse> fetched = rest.exchange(
                "/api/problems/" + first.id(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProblemDtos.ProblemResponse.class
        );
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().passed()).isTrue();
    }

    @Test
    void skipsInvalidAndEmptyItems() {
        AuthDtos.AuthResponse auth = register("ojcarol");
        HttpHeaders headers = bearer(auth.token());
        OjImportDtos.OjImportItem empty = new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF2229F",
                " ",
                " ",
                "https://codeforces.com/contest/2229/problem/F",
                false,
                null
        );
        OjImportDtos.OjImportItem invalidSourceId = new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF 2229 F",
                "Bad Source",
                "Statement",
                "https://codeforces.com/contest/2229/problem/F",
                false,
                null
        );
        OjImportDtos.OjImportItem inaccessible = new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF2229G",
                "Bad Url",
                "Statement",
                "",
                false,
                null
        );

        ResponseEntity<OjImportDtos.OjImportResponse> response = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(empty, invalidSourceId, inaccessible)), headers),
                OjImportDtos.OjImportResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).extracting(OjImportDtos.OjImportItemResult::status)
                .containsExactly("SKIPPED_EMPTY", "SKIPPED_INVALID_SOURCE_ID", "SKIPPED_INACCESSIBLE");
    }

    @Test
    void skipsDuplicateItemsWithinSameRequest() {
        AuthDtos.AuthResponse auth = register("ojdora");
        HttpHeaders headers = bearer(auth.token());

        ResponseEntity<OjImportDtos.OjImportResponse> response = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(
                        validItem("CF2230A", false),
                        validItem("CF2230A", true)
                )), headers),
                OjImportDtos.OjImportResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).extracting(OjImportDtos.OjImportItemResult::status)
                .containsExactly("QUEUED", "EXISTS_UNCHANGED");
    }

    @Test
    void skipsOversizedImportItemsBeforePersistence() {
        AuthDtos.AuthResponse auth = register("ojeve");
        HttpHeaders headers = bearer(auth.token());
        OjImportDtos.OjImportItem longTitle = new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF2230B",
                "T".repeat(221),
                "Statement",
                "https://codeforces.com/contest/2230/problem/B",
                false,
                null
        );
        OjImportDtos.OjImportItem longUrl = new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF2230C",
                "Long Url",
                "Statement",
                "https://codeforces.com/" + "x".repeat(520),
                false,
                null
        );

        ResponseEntity<OjImportDtos.OjImportResponse> response = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(longTitle, longUrl)), headers),
                OjImportDtos.OjImportResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).extracting(OjImportDtos.OjImportItemResult::status)
                .containsExactly("SKIPPED_EMPTY", "SKIPPED_INACCESSIBLE");
    }

    @Test
    void listsOjImportHistoryForCurrentUser() {
        AuthDtos.AuthResponse auth = register("ojfred");
        HttpHeaders headers = bearer(auth.token());
        importOne(headers, validItem("CF2230D", true));
        ProblemDtos.ProblemResponse problem = waitForProblem(headers, "CODEFORCES", "CF2230D");

        ResponseEntity<OjImportDtos.OjImportHistoryJob[]> response = rest.exchange(
                "/api/oj-imports/history",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OjImportDtos.OjImportHistoryJob[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        OjImportDtos.OjImportHistoryJob job = response.getBody()[0];
        assertThat(job.name()).isEqualTo("OJ 导入");
        assertThat(job.items()).hasSize(1);
        OjImportDtos.OjImportHistoryItem item = job.items().getFirst();
        assertThat(item.platform()).isEqualTo("CODEFORCES");
        assertThat(item.sourceId()).isEqualTo("CF2230D");
        assertThat(item.status()).isEqualTo("SUCCEEDED");
        assertThat(item.problemId()).isEqualTo(problem.id());
        assertThat(item.passedRequested()).isTrue();
        assertThat(item.sourceUrl()).isEqualTo("https://codeforces.com/contest/2229/problem/D");
        assertThat(item.originalStatement()).contains("You are given arrays.");
    }

    @Test
    void normalizesWowoNameInOjImportedTitleAndStatement() {
        AuthDtos.AuthResponse auth = register("ojwowo");
        HttpHeaders headers = bearer(auth.token());
        importOne(headers, new OjImportDtos.OjImportItem(
                "CODEFORCES",
                "CF2230W",
                "\u8717\u8717 and Arrays",
                "\u8717\u8717 is given arrays.\nDetermine the answer.",
                "https://codeforces.com/contest/2230/problem/W",
                false,
                null
        ));

        ProblemDtos.ProblemResponse problem = waitForProblem(headers, "CODEFORCES", "CF2230W");

        assertThat(problem.title()).isEqualTo("BOB and Arrays");
        assertThat(problem.description()).contains("BOB is given arrays.");
        assertThat(problem.description()).doesNotContain("\u8717\u8717");
    }

    private void importOne(HttpHeaders headers, OjImportDtos.OjImportItem item) {
        ResponseEntity<OjImportDtos.OjImportResponse> imported = rest.exchange(
                "/api/oj-imports",
                HttpMethod.POST,
                new HttpEntity<>(new OjImportDtos.OjImportRequest(List.of(item)), headers),
                OjImportDtos.OjImportResponse.class
        );
        assertThat(imported.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ProblemDtos.ProblemResponse waitForProblem(HttpHeaders headers, String platform, String sourceId) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            Problem problem = problems.findByExternalPlatformAndExternalSourceId(platform, sourceId).orElse(null);
            if (problem != null) {
                ResponseEntity<ProblemDtos.ProblemResponse> fetched = rest.exchange(
                    "/api/problems/" + problem.getId(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ProblemDtos.ProblemResponse.class
                );
                assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(fetched.getBody()).isNotNull();
                return fetched.getBody();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Timed out waiting for imported problem");
    }

    private OjImportDtos.OjImportItem validItem(String sourceId, boolean passed) {
        return new OjImportDtos.OjImportItem(
                "CODEFORCES",
                sourceId,
                "Median Import Test",
                "You are given arrays.\n\n\n      Determine the answer.",
                "https://codeforces.com/contest/2229/problem/D",
                passed,
                null
        );
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
