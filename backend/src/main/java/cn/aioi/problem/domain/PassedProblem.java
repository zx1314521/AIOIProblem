package cn.aioi.problem.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "passed_problems", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}))
public class PassedProblem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    private Instant passedAt;

    protected PassedProblem() {
    }

    public PassedProblem(User user, Problem problem) {
        this.user = user;
        this.problem = problem;
    }

    @PrePersist
    void prePersist() {
        if (passedAt == null) {
            passedAt = Instant.now();
        }
    }

    public Problem getProblem() {
        return problem;
    }

    public Instant getPassedAt() {
        return passedAt;
    }
}

