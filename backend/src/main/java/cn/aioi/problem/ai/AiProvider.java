package cn.aioi.problem.ai;

public interface AiProvider {
    AiAssessment assess(ProblemInput input);

    default AiAssessment assess(ProblemInput input, AiTaskType taskType) {
        return assess(input);
    }

    default String polishProblemStatement(ProblemInput input, AiTaskType taskType) {
        return input.text();
    }

    default String generateTestData(ProblemInput input) {
        throw new UnsupportedOperationException("AI data generation is not supported by this provider");
    }
}
