package cn.aioi.problem.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ProblemDataDtos {
    private ProblemDataDtos() {
    }

    public record GeneratedData(
            @NotBlank String stdCpp,
            @NotBlank String configYaml,
            @NotNull @Size(min = 25, max = 25) List<@Valid GeneratedCase> cases,
            String notes
    ) {
    }

    public record GeneratedCase(
            @Positive int index,
            @NotNull String input,
            @NotNull String output
    ) {
    }

    public record DataStatusResponse(
            Long id,
            Long problemId,
            String status,
            int caseCount,
            String errorMessage,
            String notes,
            Instant updatedAt
    ) {
    }

    public record DataSetResponse(
            Long id,
            Long problemId,
            String status,
            String stdCpp,
            String configYaml,
            String errorMessage,
            String notes,
            Instant updatedAt,
            List<DataCaseResponse> cases
    ) {
    }

    public record DataCaseResponse(
            Long id,
            int index,
            String input,
            String output
    ) {
    }

    public record DataCaseRequest(
            @Positive int index,
            @NotNull String input,
            @NotNull String output
    ) {
    }

    public record CodeRunRequest(
            @NotBlank String code,
            String input,
            List<Integer> caseIndexes
    ) {
    }

    public record CodeRunResponse(
            String status,
            String stdout,
            String stderr,
            Integer exitCode,
            long durationMs,
            List<CaseRunResponse> cases
    ) {
    }

    public record CaseRunResponse(
            int index,
            String status,
            String stdout,
            String stderr,
            String expectedOutput,
            long durationMs
    ) {
    }
}
