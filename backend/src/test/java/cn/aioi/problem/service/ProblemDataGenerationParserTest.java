package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemDataGenerationParserTest {
    private final ProblemDataGenerationParser parser = new ProblemDataGenerationParser(new ObjectMapper());

    @Test
    void parsesCodexJsonInsideMarkdownFence() {
        String json = """
                ```json
                {
                  "stdCpp": "#include <bits/stdc++.h>\\nint main(){return 0;}",
                  "configYaml": "type: default\\ntime: 1s\\nmemory: 256m",
                  "notes": "covers boundaries",
                  "cases": [
                """ + casesJson(25) + """
                  ]
                }
                ```
                """;

        ProblemDataDtos.GeneratedData parsed = parser.parse(json);

        assertThat(parsed.stdCpp()).contains("#include");
        assertThat(parsed.configYaml()).contains("type: default");
        assertThat(parsed.cases()).hasSize(25);
        assertThat(parsed.cases().getFirst().index()).isEqualTo(1);
        assertThat(parsed.notes()).isEqualTo("covers boundaries");
    }

    @Test
    void rejectsGeneratedDataWithoutTwentyFiveCases() {
        String json = """
                {"stdCpp":"int main(){}","configYaml":"type: default","cases":[
                """ + casesJson(24) + """
                ]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("25");
    }

    private String casesJson(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) {
                builder.append(",\n");
            }
            builder.append("{\"index\":")
                    .append(i)
                    .append(",\"input\":\"")
                    .append(i)
                    .append("\\n\",\"output\":\"")
                    .append(i)
                    .append("\\n\"}");
        }
        return builder.toString();
    }
}
