package com.boki.backend.domain.review.entity;

import com.boki.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "trade_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trade_reviews_trade_id", columnNames = "trade_id")
        },
        indexes = {
                @Index(name = "idx_trade_reviews_member_id", columnList = "member_id"),
                @Index(name = "idx_trade_reviews_rule_set_id", columnList = "rule_set_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeReview extends BaseEntity {

    @Id
    @Column(name = "review_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "rule_set_id", nullable = false)
    private Long ruleSetId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Builder
    private TradeReview(Long tradeId, Long memberId, Long ruleSetId, String content) {
        this.tradeId = tradeId;
        this.memberId = memberId;
        this.ruleSetId = ruleSetId;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
