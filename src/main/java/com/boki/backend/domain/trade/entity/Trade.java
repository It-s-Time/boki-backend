package com.boki.backend.domain.trade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "trades",
        indexes = {
                @Index(name = "idx_trades_member_traded_at", columnList = "member_id,traded_at"),
                @Index(name = "idx_trades_rule_set_id", columnList = "rule_set_id"),
                @Index(name = "idx_trades_external_trade", columnList = "member_id,external_trade_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @Column(name = "trade_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @Column(name = "rule_set_id")
    private Long ruleSetId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private TradeType tradeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private TradeInputType inputType;

    @Column(name = "coin_type", nullable = false, length = 50)
    private String coinType;

    @Column(name = "price", nullable = false, precision = 20, scale = 5)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 5)
    private BigDecimal totalAmount;

    @Column(name = "traded_at", nullable = false)
    private LocalDateTime tradedAt;

    @Column(name = "external_trade_id", length = 100)
    private String externalTradeId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Trade(
            Long ruleSetId,
            Long memberId,
            TradeType tradeType,
            TradeInputType inputType,
            String coinType,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal totalAmount,
            LocalDateTime tradedAt,
            String externalTradeId
    ) {
        this.ruleSetId = ruleSetId;
        this.memberId = memberId;
        this.tradeType = tradeType;
        this.inputType = inputType;
        this.coinType = coinType;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.tradedAt = tradedAt;
        this.externalTradeId = externalTradeId;
    }

    public void update(
            TradeType tradeType,
            String coinType,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal totalAmount,
            LocalDateTime tradedAt
    ) {
        if (tradeType != null) {
            this.tradeType = tradeType;
        }
        if (coinType != null) {
            this.coinType = coinType;
        }
        if (price != null) {
            this.price = price;
        }
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (totalAmount != null) {
            this.totalAmount = totalAmount;
        }
        if (tradedAt != null) {
            this.tradedAt = tradedAt;
        }
    }

    public void assignRuleSet(Long ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
