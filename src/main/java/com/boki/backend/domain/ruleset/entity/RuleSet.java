package com.boki.backend.domain.ruleset.entity;

import com.boki.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rule_sets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "set_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RuleSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_set_id")
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "set_name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 100)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "set_type", nullable = false)
    private RuleSetType type;

    @Column(name = "member_id")
    private Long memberId;

    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rule> rules = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void addRule(Rule rule) {
        this.rules.add(rule);
    }
}
