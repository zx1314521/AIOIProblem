package cn.aioi.problem.ai;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.service.TagCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssessmentParserTest {
    private final AiAssessmentParser parser = new AiAssessmentParser(new ObjectMapper(), new TagCatalogService());

    @Test
    void parsesDirectAssessmentJsonAndNormalizesTags() {
        AiAssessment assessment = parser.parse("""
                {"difficulty":"CSPS提高","confidence":0.82,"tags":["BFS","广搜","DP","图论","Segment Tree","未知"],"hints":["看状态","写转移","优化"],"reasoningSummary":"需要搜索。"}
                """);

        assertThat(assessment.difficulty()).isEqualTo(DifficultyLevel.CSPS_ADVANCED);
        assertThat(assessment.confidence()).isEqualTo(0.82);
        assertThat(assessment.tags()).containsExactly("广度优先搜索 BFS", "线段树");
        assertThat(assessment.hints()).hasSize(3);
    }

    @Test
    void parsesDeepSeekChatCompletionContent() {
        AiAssessment assessment = parser.parse("""
                {"choices":[{"message":{"content":"{\\"difficulty\\":\\"NOIP困难\\",\\"confidence\\":0.9,\\"tags\\":[\\"MCMF\\",\\"图论\\"],\\"hints\\":[\\"先想维护什么\\"],\\"reasoningSummary\\":\\"费用流建模。\\"}"}}]}
                """);

        assertThat(assessment.difficulty()).isEqualTo(DifficultyLevel.NOIP_HARD);
        assertThat(assessment.tags()).containsExactly("费用流");
    }

    @Test
    void marksUntaggedWhenNoLegalTagRemains() {
        AiAssessment assessment = parser.parse("""
                {"difficulty":"简单","confidence":0.8,"tags":["DP","图论","未知"],"hints":[],"reasoningSummary":"x"}
                """);

        assertThat(assessment.tags()).containsExactly("没有标签");
    }

    @Test
    void marksUntaggedWhenJsonCannotBeParsed() {
        AiAssessment assessment = parser.parse("not json");

        assertThat(assessment.tags()).containsExactly("没有标签");
    }
}
