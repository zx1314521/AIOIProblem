package cn.aioi.problem.repository;

import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.ProblemDataSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProblemDataSetRepository extends JpaRepository<ProblemDataSet, Long> {
    Optional<ProblemDataSet> findByProblem(Problem problem);

    Optional<ProblemDataSet> findByProblemId(Long problemId);

    @Query("select d.status from ProblemDataSet d where d.problem.id = :problemId")
    Optional<cn.aioi.problem.domain.ProblemDataStatus> findStatusByProblemId(Long problemId);
}
