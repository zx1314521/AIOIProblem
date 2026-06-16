package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.ProblemDataCase;
import cn.aioi.problem.domain.ProblemDataSet;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.ProblemDataSetRepository;
import cn.aioi.problem.repository.ProblemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ProblemDataService {
    private final ProblemRepository problems;
    private final ProblemDataSetRepository dataSets;
    private final CppExecutionService executionService;
    private final BatchJobService batchJobService;

    public ProblemDataService(ProblemRepository problems, ProblemDataSetRepository dataSets,
                              CppExecutionService executionService, BatchJobService batchJobService) {
        this.problems = problems;
        this.dataSets = dataSets;
        this.executionService = executionService;
        this.batchJobService = batchJobService;
    }

    @Transactional(readOnly = true)
    public ProblemDataDtos.DataStatusResponse status(Long problemId, User user) {
        Problem problem = problem(problemId, user);
        return dataSets.findByProblem(problem)
                .map(this::toStatus)
                .orElse(new ProblemDataDtos.DataStatusResponse(null, problem.getId(), "NONE", 0, null, null, null));
    }

    @Transactional(readOnly = true)
    public ProblemDataDtos.DataSetResponse detail(Long problemId, User user) {
        Problem problem = problem(problemId, user);
        return dataSets.findByProblem(problem)
                .map(this::toResponse)
                .orElse(new ProblemDataDtos.DataSetResponse(null, problem.getId(), "NONE", "", "", null, null, null, List.of()));
    }

    @Transactional
    public ProblemDataDtos.DataStatusResponse startGeneration(Long problemId, User user) {
        Problem problem = problem(problemId, user);
        ProblemDataSet dataSet = dataSets.findByProblem(problem).orElseGet(() -> dataSets.save(new ProblemDataSet(problem)));
        dataSet.generating();
        ProblemDataDtos.DataStatusResponse response = toStatus(dataSet);
        batchJobService.enqueueDataGeneration(problem, user);
        return response;
    }

    @Transactional
    public ProblemDataDtos.DataSetResponse addCase(Long problemId, User user, ProblemDataDtos.DataCaseRequest request) {
        Problem problem = problem(problemId, user);
        ProblemDataSet dataSet = dataSets.findByProblem(problem).orElseGet(() -> dataSets.save(new ProblemDataSet(problem)));
        dataSet.upsertCase(request.index(), request.input(), request.output());
        return toResponse(dataSet);
    }

    @Transactional
    public ProblemDataDtos.DataSetResponse updateCase(Long problemId, User user, Long caseId, ProblemDataDtos.DataCaseRequest request) {
        ProblemDataSet dataSet = ownedDataSet(problemId, user);
        dataSet.updateCase(caseId, request.index(), request.input(), request.output());
        return toResponse(dataSet);
    }

    @Transactional
    public ProblemDataDtos.DataSetResponse deleteCase(Long problemId, User user, Long caseId) {
        ProblemDataSet dataSet = ownedDataSet(problemId, user);
        dataSet.removeCase(caseId);
        return toResponse(dataSet);
    }

    @Transactional(readOnly = true)
    public ProblemDataDtos.CodeRunResponse runDebug(Long problemId, User user, ProblemDataDtos.CodeRunRequest request) {
        problem(problemId, user);
        return executionService.runDebug(request.code(), request.input());
    }

    @Transactional(readOnly = true)
    public ProblemDataDtos.CodeRunResponse runCases(Long problemId, User user, ProblemDataDtos.CodeRunRequest request) {
        ProblemDataSet dataSet = ownedDataSet(problemId, user);
        List<Integer> selected = request.caseIndexes() == null ? List.of() : request.caseIndexes();
        List<ProblemDataDtos.DataCaseResponse> cases = dataSet.getCases().stream()
                .filter(item -> selected.isEmpty() || selected.contains(item.getIndex()))
                .map(this::toCaseResponse)
                .toList();
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("没有可运行的测试点");
        }
        return executionService.runCases(request.code(), cases);
    }

    @Transactional(readOnly = true)
    public byte[] zip(Long problemId, User user) {
        ProblemDataSet dataSet = ownedDataSet(problemId, user);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                addZipEntry(zip, "std.cpp", dataSet.getStdCpp());
                addZipEntry(zip, "config.yaml", dataSet.getConfigYaml());
                for (ProblemDataCase item : dataSet.getCases()) {
                    String prefix = String.format("%02d", item.getIndex());
                    addZipEntry(zip, prefix + ".in", item.getInput());
                    addZipEntry(zip, prefix + ".out", item.getOutput());
                }
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("测试数据打包失败: " + exception.getMessage(), exception);
        }
    }

    private Problem problem(Long problemId, User user) {
        return problems.findById(problemId).orElseThrow(() -> new EntityNotFoundException("题目不存在"));
    }
    private ProblemDataSet dataSet(Long problemId) {
        return dataSets.findByProblemId(problemId).orElseThrow(() -> new EntityNotFoundException("题目数据不存在"));
    }

    private ProblemDataSet ownedDataSet(Long problemId, User user) {
        problem(problemId, user);
        return dataSet(problemId);
    }

    private ProblemDataDtos.DataStatusResponse toStatus(ProblemDataSet dataSet) {
        return new ProblemDataDtos.DataStatusResponse(
                dataSet.getId(),
                dataSet.getProblem().getId(),
                dataSet.getStatus().name(),
                dataSet.getCases().size(),
                dataSet.getErrorMessage(),
                dataSet.getNotes(),
                updatedAt(dataSet)
        );
    }

    private ProblemDataDtos.DataSetResponse toResponse(ProblemDataSet dataSet) {
        return new ProblemDataDtos.DataSetResponse(
                dataSet.getId(),
                dataSet.getProblem().getId(),
                dataSet.getStatus().name(),
                nullToEmpty(dataSet.getStdCpp()),
                nullToEmpty(dataSet.getConfigYaml()),
                dataSet.getErrorMessage(),
                dataSet.getNotes(),
                updatedAt(dataSet),
                dataSet.getCases().stream().map(this::toCaseResponse).toList()
        );
    }

    private ProblemDataDtos.DataCaseResponse toCaseResponse(ProblemDataCase item) {
        return new ProblemDataDtos.DataCaseResponse(item.getId(), item.getIndex(), item.getInput(), item.getOutput());
    }

    private Instant updatedAt(ProblemDataSet dataSet) {
        return dataSet.getUpdatedAt() == null ? Instant.now() : dataSet.getUpdatedAt();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void addZipEntry(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(nullToEmpty(value).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
