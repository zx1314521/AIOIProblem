package cn.aioi.problem.ai;

public interface AiProvider {
    AiAssessment assess(ProblemInput input);

    default AiAssessment assess(ProblemInput input, AiTaskType taskType) {
        return assess(input);
    }

    default String polishProblemStatement(ProblemInput input, AiTaskType taskType) {
        return input.text();
    }
}
