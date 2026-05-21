package cn.aioi.problem.repository;

import cn.aioi.problem.domain.ProblemSet;
import cn.aioi.problem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, Long> {
    List<ProblemSet> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<ProblemSet> findByIdAndOwner(Long id, User owner);
}
