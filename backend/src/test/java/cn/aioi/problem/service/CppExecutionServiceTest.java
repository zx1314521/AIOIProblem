package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CppExecutionServiceTest {
    private final CppExecutionService service = new CppExecutionService();

    @Test
    void compilesAndRunsDebugInput() {
        ProblemDataDtos.CodeRunResponse response = service.runDebug("""
                #include <bits/stdc++.h>
                using namespace std;
                int main() {
                    long long a, b;
                    cin >> a >> b;
                    cout << a + b << "\\n";
                    return 0;
                }
                """, "2 3\n");

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.stdout()).isEqualTo("5\n");
        assertThat(response.stderr()).isEmpty();
        assertThat(response.exitCode()).isEqualTo(0);
    }

    @Test
    void returnsCompileErrorWithoutRunning() {
        ProblemDataDtos.CodeRunResponse response = service.runDebug("int main(){ syntax error }\n", "");

        assertThat(response.status()).isEqualTo("CE");
        assertThat(response.stderr()).contains("error");
        assertThat(response.exitCode()).isNull();
    }

    @Test
    void runsCasesAndComparesTrimmedOutput() {
        List<ProblemDataDtos.DataCaseResponse> cases = List.of(
                new ProblemDataDtos.DataCaseResponse(1L, 1, "1 2\n", "3\n"),
                new ProblemDataDtos.DataCaseResponse(2L, 2, "2 2\n", "5\n")
        );

        ProblemDataDtos.CodeRunResponse response = service.runCases("""
                #include <bits/stdc++.h>
                using namespace std;
                int main() {
                    int a, b;
                    cin >> a >> b;
                    cout << a + b << "\\n";
                }
                """, cases);

        assertThat(response.status()).isEqualTo("WA");
        assertThat(response.cases()).extracting(ProblemDataDtos.CaseRunResponse::status)
                .containsExactly("AC", "WA");
    }
}
