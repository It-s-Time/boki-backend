package com.boki.backend.domain.ruleset.service;

import com.boki.backend.domain.ruleset.dto.request.*;
import com.boki.backend.domain.ruleset.dto.response.RuleSetResDTO;
import com.boki.backend.domain.ruleset.entity.*;
import com.boki.backend.domain.ruleset.repository.RuleRepository;
import com.boki.backend.domain.ruleset.repository.RuleSetRepository;
import com.boki.backend.domain.ruleset.exception.RuleSetErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleSetService {

    private final RuleSetRepository ruleSetRepository;
    private final RuleRepository ruleRepository;

    public List<RuleSetResDTO> getRuleSets(Long userId, String type) {
        List<RuleSet> ruleSets;

        if ("template".equalsIgnoreCase(type)) {
            ruleSets = ruleSetRepository.findByType(RuleSetType.TEMPLATE);
        } else if ("custom".equalsIgnoreCase(type)) {
            ruleSets = ruleSetRepository.findByMemberIdAndType(userId, RuleSetType.CUSTOM);
        } else {
            ruleSets = ruleSetRepository.findByMemberId(userId);
        }

        return ruleSets.stream().map(RuleSetResDTO::from).toList();
    }

    public RuleSetResDTO getRuleSet(Long userId, Long ruleSetId) {
        return RuleSetResDTO.from(findRuleSetForRead(userId, ruleSetId));
    }

    @Transactional
    public RuleSetResDTO createRuleSet(@NonNull Long userId, RuleSetCreateReqDTO request) {
        RuleSet ruleSet = RuleSet.builder()
                .memberId(userId)
                .name(request.getName())
                .type(RuleSetType.CUSTOM)
                .build();

        return RuleSetResDTO.from(ruleSetRepository.save(ruleSet));
    }

    @Transactional
    public RuleSetResDTO updateRuleSet(Long userId, Long ruleSetId, RuleSetUpdateReqDTO request) {
        RuleSet ruleSet = findRuleSetByUser(userId, ruleSetId);
        ruleSet.updateName(request.getName());
        return RuleSetResDTO.from(ruleSet);
    }

    @Transactional
    public void deleteRuleSet(Long userId, Long ruleSetId) {
        ruleSetRepository.delete(findRuleSetByUser(userId, ruleSetId));
    }

    @Transactional
    public RuleSetResDTO copyFromTemplate(Long userId, Long templateId, RuleSetCopyReqDTO request) {
        RuleSet template = ruleSetRepository.findById(templateId)
                .filter(rs -> rs.getType() == RuleSetType.TEMPLATE)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_SET_NOT_FOUND));

        if (ruleSetRepository.findByMemberIdAndTemplateId(userId, templateId).isPresent()) {
            throw new GeneralException(RuleSetErrorCode.RULE_SET_ALREADY_EXISTS);
        }

        RuleSet customCopy = RuleSet.builder()
                .memberId(userId)
                .name(request.getName())
                .type(RuleSetType.CUSTOM)
                .templateId(template.getId())
                .build();

        RuleSet savedCopy = ruleSetRepository.save(customCopy);

        template.getRules().stream()
                .filter(Rule::isActive)
                .forEach(templateRule -> savedCopy.addRule(Rule.builder()
                        .ruleSet(savedCopy)
                        .type(templateRule.getType())
                        .content(templateRule.getContent())
                        .orderIndex(templateRule.getOrderIndex())
                        .build()));

        return RuleSetResDTO.from(savedCopy);
    }

    @Transactional
    public RuleSetResDTO createRuleSetWithRules(Long userId, RuleSetWithRulesCreateReqDTO request) {
        if (ruleSetRepository.existsByMemberIdAndName(userId, request.getName())) {
            throw new GeneralException(RuleSetErrorCode.RULE_SET_ALREADY_EXISTS);
        }

        RuleSet ruleSet = RuleSet.builder()
                .memberId(userId)
                .name(request.getName())
                .type(RuleSetType.CUSTOM)
                .build();

        RuleSet savedRuleSet = ruleSetRepository.save(ruleSet);

        if (request.getBuyRules() != null) {
            for (int i = 0; i < request.getBuyRules().size(); i++) {
                savedRuleSet.addRule(Rule.builder()
                        .ruleSet(savedRuleSet)
                        .type(RuleType.BUY)
                        .content(request.getBuyRules().get(i))
                        .orderIndex(i)
                        .build());
            }
        }

        if (request.getSellRules() != null) {
            for (int i = 0; i < request.getSellRules().size(); i++) {
                savedRuleSet.addRule(Rule.builder()
                        .ruleSet(savedRuleSet)
                        .type(RuleType.SELL)
                        .content(request.getSellRules().get(i))
                        .orderIndex(i)
                        .build());
            }
        }

        ruleSetRepository.flush();
        return RuleSetResDTO.from(savedRuleSet);
    }

    @Transactional
    public RuleSetResDTO addBuyRule(Long userId, Long ruleSetId, RuleCreateReqDTO request) {
        return addRule(userId, ruleSetId, request, RuleType.BUY);
    }

    @Transactional
    public RuleSetResDTO addSellRule(Long userId, Long ruleSetId, RuleCreateReqDTO request) {
        return addRule(userId, ruleSetId, request, RuleType.SELL);
    }

    @Transactional
    public RuleSetResDTO updateRule(Long userId, Long ruleSetId, Long ruleId, RuleUpdateReqDTO request) {
        findRuleSetByUser(userId, ruleSetId);
        Rule rule = ruleRepository.findByIdAndRuleSetIdAndIsActiveTrue(ruleId, ruleSetId)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_NOT_FOUND));
        rule.update(request.getContent());
        return RuleSetResDTO.from(ruleSetRepository.findById(ruleSetId).get());
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleSetId, Long ruleId) {
        findRuleSetByUser(userId, ruleSetId);
        Rule rule = ruleRepository.findByIdAndRuleSetIdAndIsActiveTrue(ruleId, ruleSetId)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_NOT_FOUND));
        rule.deactivate();
    }

    // 템플릿은 소유자 체크 없이 조회 가능
    private RuleSet findRuleSetForRead(Long userId, Long ruleSetId) {
        RuleSet ruleSet = ruleSetRepository.findById(ruleSetId)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_SET_NOT_FOUND));

        if (ruleSet.getType() == RuleSetType.CUSTOM && !ruleSet.getMemberId().equals(userId)) {
            throw new GeneralException(RuleSetErrorCode.RULE_SET_FORBIDDEN);
        }

        return ruleSet;
    }

    // 수정/삭제는 내 CUSTOM 세트만 가능
    private RuleSet findRuleSetByUser(Long userId, Long ruleSetId) {
        return ruleSetRepository.findByIdAndMemberId(ruleSetId, userId)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_SET_NOT_FOUND));
    }

    private RuleSetResDTO addRule(Long userId, Long ruleSetId, RuleCreateReqDTO request, RuleType ruleType) {
        RuleSet originalRuleSet = ruleSetRepository.findById(ruleSetId)
                .orElseThrow(() -> new GeneralException(RuleSetErrorCode.RULE_SET_NOT_FOUND));

        RuleSet targetRuleSet;

        if (originalRuleSet.getType() == RuleSetType.TEMPLATE) {
            // 내 복사본이 있으면 거기에 추가, 없으면 복사본 생성
            targetRuleSet = ruleSetRepository.findByMemberIdAndTemplateId(userId, ruleSetId)
                    .orElseGet(() -> createCustomCopyFromTemplate(userId, originalRuleSet));
        } else {
            // CUSTOM 세트면 소유자 확인 후 추가
            targetRuleSet = findRuleSetByUser(userId, ruleSetId);
        }

        int orderIndex = ruleRepository.countByRuleSetIdAndTypeAndIsActiveTrue(targetRuleSet.getId(), ruleType);

        Rule rule = Rule.builder()
                .ruleSet(targetRuleSet)
                .type(ruleType)
                .content(request.getContent())
                .orderIndex(orderIndex)
                .build();

        targetRuleSet.addRule(rule);
        return RuleSetResDTO.from(targetRuleSet);
    }

    private RuleSet createCustomCopyFromTemplate(Long userId, RuleSet template) {
        RuleSet customCopy = RuleSet.builder()
                .memberId(userId)
                .name(template.getName())
                .type(RuleSetType.CUSTOM)
                .templateId(template.getId())
                .build();

        RuleSet savedCopy = ruleSetRepository.save(customCopy);

        // 템플릿의 원칙들도 그대로 복사
        template.getRules().stream()
                .filter(Rule::isActive)
                .forEach(templateRule -> {
                    Rule copiedRule = Rule.builder()
                            .ruleSet(savedCopy)
                            .type(templateRule.getType())
                            .content(templateRule.getContent())
                            .orderIndex(templateRule.getOrderIndex())
                            .build();
                    savedCopy.addRule(copiedRule);
                });

        return savedCopy;
    }
}
