package com.boki.backend.domain.ai.repository;

import com.boki.backend.domain.ai.entity.AiReport;
import com.boki.backend.domain.ai.entity.ReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findByTradeId(Long tradeId);

    boolean existsByTradeId(Long tradeId);

    boolean existsByTradeIdAndStatusIn(Long tradeId, List<ReportStatus> statuses);
}
