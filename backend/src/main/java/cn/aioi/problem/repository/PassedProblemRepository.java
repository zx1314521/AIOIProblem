package cn.aioi.problem.repository;

import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassedProblemRepository extends JpaRepository<PassedProblem, Long> {
    boolean existsByUserAndProblem(User user, Problem problem);

    Optional<PassedProblem> findByUserAndProblem(User user, Problem problem);

    List<PassedProblem> findByUser(User user);
}

