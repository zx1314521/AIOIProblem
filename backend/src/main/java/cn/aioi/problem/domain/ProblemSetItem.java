package cn.aioi.problem.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "problem_set_items", uniqueConstraints = @UniqueConstraint(columnNames = {"problem_set_id", "problem_id"}))
public class ProblemSetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_set_id", nullable = false)
    private ProblemSet problemSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    private int sortOrder;

    protected ProblemSetItem() {
    }

    public ProblemSetItem(ProblemSet problemSet, Problem problem, int sortOrder) {
        this.problemSet = problemSet;
        this.problem = problem;
        this.sortOrder = sortOrder;
    }

    public Problem getProblem() {
        return problem;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}

