package cn.aioi.problem.repository;

import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.ProblemSetItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetItemRepository extends JpaRepository<ProblemSetItem, Long> {
    void deleteByProblem(Problem problem);
}
