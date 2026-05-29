package cn.aioi.problem.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "problems", uniqueConstraints = @UniqueConstraint(columnNames = {"external_platform", "external_source_id"}))
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DifficultyLevel difficulty;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag", length = 64)
    private Set<String> tags = new LinkedHashSet<>();

    private String source;

    @Column(length = 32)
    private String externalPlatform;

    @Column(length = 80)
    private String externalSourceId;

    @Column(length = 512)
    private String sourceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected Problem() {
    }

    public Problem(String title, String description, DifficultyLevel difficulty, Set<String> tags, String source, User createdBy) {
        this(title, description, difficulty, tags, source, createdBy, null, null, null);
    }

    public Problem(String title, String description, DifficultyLevel difficulty, Set<String> tags, String source,
                   User createdBy, String externalPlatform, String externalSourceId, String sourceUrl) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.tags = new LinkedHashSet<>(tags);
        this.source = source;
        this.createdBy = createdBy;
        this.externalPlatform = externalPlatform;
        this.externalSourceId = externalSourceId;
        this.sourceUrl = sourceUrl;
    }

    public void update(String title, String description, DifficultyLevel difficulty, Set<String> tags, String source) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.tags = new LinkedHashSet<>(tags);
        this.source = source;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getSource() {
        return source;
    }

    public String getExternalPlatform() {
        return externalPlatform;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
