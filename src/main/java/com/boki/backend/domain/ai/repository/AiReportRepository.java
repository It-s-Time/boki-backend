package com.boki.backend.domain.ai.repository;

import com.boki.backend.domain.ai.entity.AiReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findByTradeId(Long tradeId);

    boolean existsByTradeId(Long tradeId);
}
