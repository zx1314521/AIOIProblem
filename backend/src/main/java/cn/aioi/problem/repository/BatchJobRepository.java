package cn.aioi.problem.repository;

import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobStatus;
import cn.aioi.problem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {
    List<BatchJob> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<BatchJob> findByIdAndOwner(Long id, User owner);

    List<BatchJob> findByStatusOrderByCreatedAtAsc(BatchJobStatus status);
}

