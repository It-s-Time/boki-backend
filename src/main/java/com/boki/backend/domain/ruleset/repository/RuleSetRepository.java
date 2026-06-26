//
package com.boki.backend.domain.ruleset.repository;

import com.boki.backend.domain.ruleset.entity.RuleSet;
import com.boki.backend.domain.ruleset.entity.RuleSetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleSetRepository extends JpaRepository<RuleSet, Long> {

    List<RuleSet> findByType(RuleSetType type);

    List<RuleSet> findByMemberIdAndType(Long memberId, RuleSetType type);

    List<RuleSet> findByMemberId(Long memberId);

    Optional<RuleSet> findByIdAndMemberId(Long id, Long memberId);

    // 유저의 특정 템플릿 복사본 조회
    Optional<RuleSet> findByMemberIdAndTemplateId(Long memberId, Long templateId);

    boolean existsByMemberIdAndName(Long memberId, String name);
}
