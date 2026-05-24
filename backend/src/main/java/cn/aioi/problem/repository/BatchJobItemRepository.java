package cn.aioi.problem.repository;

import cn.aioi.problem.domain.BatchItemStatus;
import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchJobItemRepository extends JpaRepository<BatchJobItem, Long> {
    List<BatchJobItem> findByJobOrderByIdAsc(BatchJob job);

    List<BatchJobItem> findByJobOrderBySortOrderAscIdAsc(BatchJob job);

    Optional<BatchJobItem> findFirstByJobAndStatusOrderByIdAsc(BatchJob job, BatchItemStatus status);

    Optional<BatchJobItem> findFirstByJobAndStatusOrderBySortOrderAscIdAsc(BatchJob job, BatchItemStatus status);

    long countByJobAndStatus(BatchJob job, BatchItemStatus status);
}
