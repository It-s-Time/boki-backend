package com.boki.backend.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "review_scores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_scores_review_rule", columnNames = {"review_id", "rule_id"})
        },
        indexes = {
                @Index(name = "idx_review_scores_rule_id", columnList = "rule_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewScore {

    @Id
    @Column(name = "review_score_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewScoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private TradeReview review;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Builder
    private ReviewScore(TradeReview review, Long ruleId, Integer score) {
        this.review = review;
        this.ruleId = ruleId;
        this.score = score;
    }
}
