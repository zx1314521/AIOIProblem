package cn.aioi.problem.ai;

import cn.aioi.problem.domain.DifficultyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssessmentParserTest {
    private final AiAssessmentParser parser = new AiAssessmentParser(new ObjectMapper());

    @Test
    void parsesDirectAssessmentJson() {
        AiAssessment assessment = parser.parse("""
                {"difficulty":"CSPS提高","confidence":0.82,"tags":["动态规划","图论"],"hints":["看状态","写转移","优化"],"reasoningSummary":"需要 DP。"}
                """);

        assertThat(assessment.difficulty()).isEqualTo(DifficultyLevel.CSPS_ADVANCED);
        assertThat(assessment.confidence()).isEqualTo(0.82);
        assertThat(assessment.tags()).containsExactly("动态规划", "图论");
        assertThat(assessment.hints()).hasSize(3);
    }

    @Test
    void parsesDeepSeekChatCompletionContent() {
        AiAssessment assessment = parser.parse("""
                {"choices":[{"message":{"content":"{\\"difficulty\\":\\"NOIP困难\\",\\"confidence\\":0.9,\\"tags\\":[\\"数据结构\\"],\\"hints\\":[\\"先想维护什么\\"],\\"reasoningSummary\\":\\"线段树维护。\\"}"}}]}
                """);

        assertThat(assessment.difficulty()).isEqualTo(DifficultyLevel.NOIP_HARD);
        assertThat(assessment.tags()).containsExactly("数据结构");
    }
}

