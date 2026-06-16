package cn.aioi.problem.domain;

import jakarta.persistence.Column;
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
@Table(name = "problem_data_cases", uniqueConstraints = @UniqueConstraint(columnNames = {"data_set_id", "case_index"}))
public class ProblemDataCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_set_id", nullable = false)
    private ProblemDataSet dataSet;

    @Column(name = "case_index", nullable = false)
    private int index;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String output;

    protected ProblemDataCase() {
    }

    public ProblemDataCase(ProblemDataSet dataSet, int index, String input, String output) {
        this.dataSet = dataSet;
        this.index = index;
        this.input = input;
        this.output = output;
    }

    void attachTo(ProblemDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public void update(int index, String input, String output) {
        this.index = index;
        this.input = input;
        this.output = output;
    }

    public Long getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }
}
