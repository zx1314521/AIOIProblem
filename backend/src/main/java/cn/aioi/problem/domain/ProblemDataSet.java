package cn.aioi.problem.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "problem_data_sets")
public class ProblemDataSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false, unique = true)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ProblemDataStatus status = ProblemDataStatus.GENERATING;

    @Column(columnDefinition = "TEXT")
    private String stdCpp;

    @Column(columnDefinition = "TEXT")
    private String configYaml;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "dataSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemDataCase> cases = new ArrayList<>();

    protected ProblemDataSet() {
    }

    public ProblemDataSet(Problem problem) {
        this.problem = problem;
    }

    public void generating() {
        this.status = ProblemDataStatus.GENERATING;
        this.errorMessage = null;
    }

    public void fail(String message) {
        this.status = ProblemDataStatus.FAILED;
        this.errorMessage = message;
    }

    public void replaceGeneratedData(String stdCpp, String configYaml, String notes, List<ProblemDataCase> newCases) {
        this.status = ProblemDataStatus.READY;
        this.stdCpp = stdCpp;
        this.configYaml = configYaml;
        this.notes = notes;
        this.errorMessage = null;
        this.cases.clear();
        newCases.forEach(this::addCase);
    }

    public ProblemDataCase upsertCase(int index, String input, String output) {
        Optional<ProblemDataCase> existing = cases.stream()
                .filter(item -> item.getIndex() == index)
                .findFirst();
        if (existing.isPresent()) {
            existing.get().update(index, input, output);
            this.status = ProblemDataStatus.READY;
            this.errorMessage = null;
            return existing.get();
        }
        ProblemDataCase item = new ProblemDataCase(this, index, input, output);
        cases.add(item);
        this.status = ProblemDataStatus.READY;
        this.errorMessage = null;
        return item;
    }

    public void updateCase(Long caseId, int index, String input, String output) {
        ProblemDataCase item = cases.stream()
                .filter(candidate -> candidate.getId().equals(caseId))
                .findFirst()
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("测试点不存在"));
        item.update(index, input, output);
        this.status = ProblemDataStatus.READY;
        this.errorMessage = null;
    }

    public void removeCase(Long caseId) {
        boolean removed = cases.removeIf(item -> item.getId().equals(caseId));
        if (!removed) {
            throw new jakarta.persistence.EntityNotFoundException("测试点不存在");
        }
        this.status = ProblemDataStatus.READY;
        this.errorMessage = null;
    }

    private void addCase(ProblemDataCase item) {
        item.attachTo(this);
        cases.add(item);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Problem getProblem() {
        return problem;
    }

    public ProblemDataStatus getStatus() {
        return status;
    }

    public String getStdCpp() {
        return stdCpp;
    }

    public String getConfigYaml() {
        return configYaml;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ProblemDataCase> getCases() {
        return cases.stream()
                .sorted(Comparator.comparingInt(ProblemDataCase::getIndex))
                .toList();
    }
}
