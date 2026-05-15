package com.boki.backend.domain.ruleset.dto.response;

import com.boki.backend.domain.ruleset.entity.RuleSet;
import com.boki.backend.domain.ruleset.entity.RuleSetType;
import com.boki.backend.domain.ruleset.entity.RuleType;
import lombok.*;


import java.util.List;

@Getter
@Builder
public class RuleSetResDTO {

    private Long ruleSetId;
    private String name;
    private RuleSetType type;
    private List<RuleResDTO> buyRules;
    private List<RuleResDTO> sellRules;

    public static RuleSetResDTO from(RuleSet ruleSet) {
        return RuleSetResDTO.builder()
                .ruleSetId(ruleSet.getId())
                .name(ruleSet.getName())
                .type(ruleSet.getType())
                .buyRules(ruleSet.getRules().stream()
                        .filter(r -> r.getType() == RuleType.BUY && r.isActive())
                        .map(RuleResDTO::from)
                        .toList())
                .sellRules(ruleSet.getRules().stream()
                        .filter(r -> r.getType() == RuleType.SELL && r.isActive())
                        .map(RuleResDTO::from)
                        .toList())
                .build();
    }
}
