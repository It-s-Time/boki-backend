package com.boki.backend.domain.ai.repository;

import com.boki.backend.domain.ai.entity.AiReport;
import com.boki.backend.domain.ai.entity.Grade;
import com.boki.backend.domain.ai.entity.ReportStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findByTradeId(Long tradeId);

    boolean existsByTradeId(Long tradeId);

    boolean existsByTradeIdAndStatusIn(Long tradeId, List<ReportStatus> statuses);

    @Query("SELECT ar.tradeId, ar.grade FROM AiReport ar " +
           "WHERE ar.tradeId IN :tradeIds AND ar.status = 'COMPLETED'")
    List<Object[]> findTradeIdAndGradeByTradeIdInAndStatusCompleted(@Param("tradeIds") List<Long> tradeIds);

    default Map<Long, Grade> findGradeMapByTradeIds(List<Long> tradeIds) {
        return findTradeIdAndGradeByTradeIdInAndStatusCompleted(tradeIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Grade) r[1]));
    }
}
