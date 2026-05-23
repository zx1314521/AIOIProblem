package cn.aioi.problem.repository;

import cn.aioi.problem.domain.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
}

