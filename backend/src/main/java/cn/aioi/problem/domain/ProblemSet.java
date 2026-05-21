package cn.aioi.problem.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "problem_sets")
public class ProblemSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "problemSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemSetItem> items = new ArrayList<>();

    private Instant createdAt;

    protected ProblemSet() {
    }

    public ProblemSet(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addProblem(Problem problem) {
        boolean exists = items.stream().anyMatch(item -> item.getProblem().getId().equals(problem.getId()));
        if (!exists) {
            items.add(new ProblemSetItem(this, problem, items.size()));
        }
    }

    public void removeProblem(Long problemId) {
        items.removeIf(item -> item.getProblem().getId().equals(problemId));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ProblemSetItem> getItems() {
        return items.stream().sorted(Comparator.comparingInt(ProblemSetItem::getSortOrder)).toList();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

