package com.boki.backend.domain.ruleset.repository;

import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.entity.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule, Long> {

    Optional<Rule> findByIdAndRuleSetIdAndIsActiveTrue(Long id, Long ruleSetId);

    int countByRuleSetIdAndTypeAndIsActiveTrue(Long ruleSetId, RuleType type);

    List<Rule> findByRuleSetIdAndIsActiveTrueOrderByOrderIndexAsc(Long ruleSetId);

    List<Rule> findByRuleSetIdAndTypeAndIsActiveTrueOrderByOrderIndexAsc(Long ruleSetId, RuleType type);
}
