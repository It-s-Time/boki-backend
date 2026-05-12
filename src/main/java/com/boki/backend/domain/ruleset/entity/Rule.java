package com.boki.backend.domain.ruleset.entity;

import com.boki.backend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Rule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;

    public void update(String content) {
        this.content = content;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
