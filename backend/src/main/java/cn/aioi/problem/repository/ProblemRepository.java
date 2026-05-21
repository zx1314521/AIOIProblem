package cn.aioi.problem.repository;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    @Query("""
            select distinct p from Problem p left join p.tags t
            where (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
                or lower(p.description) like lower(concat('%', :keyword, '%')))
              and (:difficulty is null or p.difficulty = :difficulty)
              and (:tag is null or lower(t) = lower(:tag))
            order by p.createdAt desc
            """)
    List<Problem> search(@Param("keyword") String keyword,
                         @Param("difficulty") DifficultyLevel difficulty,
                         @Param("tag") String tag);

    @Query("select distinct p from Problem p left join fetch p.tags")
    List<Problem> findAllWithTags();
}

