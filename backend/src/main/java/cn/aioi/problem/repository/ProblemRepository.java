package cn.aioi.problem.repository;

import cn.aioi.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    @Query("select distinct p from Problem p left join fetch p.tags")
    List<Problem> findAllWithTags();
}
