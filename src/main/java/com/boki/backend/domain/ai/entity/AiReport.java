package com.boki.backend.domain.ai.entity;

import com.boki.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_report_id")
    private Long aiReportId;

    @Column(name = "trade_id", nullable = false, unique = true)
    private Long tradeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", length = 1)
    private Grade grade;

    @Column(name = "compliance_rate")
    private Double complianceRate;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Builder
    private AiReport(Long tradeId, Long memberId) {
        this.tradeId = tradeId;
        this.memberId = memberId;
        this.status = ReportStatus.PENDING;
    }

    public void complete(String content, Double complianceRate, Grade grade) {
        this.status = ReportStatus.COMPLETED;
        this.content = content;
        this.complianceRate = complianceRate;
        this.grade = grade;
    }

    public void fail() {
        this.status = ReportStatus.FAILED;
    }

    public void reset() {
        this.status = ReportStatus.PENDING;
        this.content = null;
        this.complianceRate = null;
        this.grade = null;
    }
}
