package com.boki.backend.domain.ruleset.controller;

import com.boki.backend.domain.ruleset.dto.request.*;
import com.boki.backend.domain.ruleset.dto.response.RuleSetResDTO;
import com.boki.backend.domain.ruleset.service.RuleSetService;
import com.boki.backend.global.auth.AuthenticatedUserProvider;
import com.boki.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "매매원칙 세트", description = "매매원칙 세트(룰셋) 관련 API")
@RestController
@RequestMapping("/api/rule-sets")
@RequiredArgsConstructor
public class RuleSetController {

    private final RuleSetService ruleSetService;
    private final AuthenticatedUserProvider userProvider;

    @Operation(summary = "세트 목록 조회", description = "type: template(템플릿) | custom(내 세트) | 생략 시 전체 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleSetResDTO>>> getRuleSets(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.getRuleSets(userProvider.getCurrentUserId(), type)));
    }

    @Operation(summary = "세트 상세 조회", description = "룰셋 ID로 단건 조회. 템플릿은 누구나, 커스텀은 본인 것만 가능")
    @GetMapping("/{ruleSetId}")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> getRuleSet(@PathVariable Long ruleSetId) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.getRuleSet(userProvider.getCurrentUserId(), ruleSetId)));
    }

    @Operation(summary = "템플릿으로 세트 생성", description = "추천 템플릿을 복사해 내 커스텀 세트 생성 (온보딩 시작하기)")
    @PostMapping("/from-template/{ruleSetId}")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> copyFromTemplate(
            @PathVariable Long ruleSetId,
            @RequestBody @Valid RuleSetCopyReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.copyFromTemplate(userProvider.getCurrentUserId(), ruleSetId, request)));
    }

    @Operation(summary = "세트 생성", description = "새 커스텀 룰셋 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<RuleSetResDTO>> createRuleSet(
            @RequestBody @Valid RuleSetCreateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.createRuleSet(userProvider.getCurrentUserId(), request)));
    }

    @Operation(summary = "직접 추가하기 세트 생성", description = "세트 생성과 매수/매도 원칙 추가를 한 번에 처리")
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> createRuleSetWithRules(
            @RequestBody @Valid RuleSetWithRulesCreateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.createRuleSetWithRules(userProvider.getCurrentUserId(), request)));
    }

    @Operation(summary = "세트 수정", description = "내 커스텀 룰셋 이름 수정")
    @PatchMapping("/{ruleSetId}")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> updateRuleSet(
            @PathVariable Long ruleSetId,
            @RequestBody @Valid RuleSetUpdateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.updateRuleSet(userProvider.getCurrentUserId(), ruleSetId, request)));
    }

    @Operation(summary = "세트 삭제", description = "내 커스텀 룰셋 삭제")
    @DeleteMapping("/{ruleSetId}")
    public ResponseEntity<ApiResponse<Void>> deleteRuleSet(@PathVariable Long ruleSetId) {
        ruleSetService.deleteRuleSet(userProvider.getCurrentUserId(), ruleSetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "매수 원칙 추가", description = "룰셋에 매수 원칙 추가. 템플릿 ID 입력 시 자동으로 내 커스텀 복사본 생성 후 추가")
    @PostMapping("/{ruleSetId}/buy-rules")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> addBuyRule(
            @PathVariable Long ruleSetId,
            @RequestBody @Valid RuleCreateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.addBuyRule(userProvider.getCurrentUserId(), ruleSetId, request)));
    }

    @Operation(summary = "매도 원칙 추가", description = "룰셋에 매도 원칙 추가. 템플릿 ID 입력 시 자동으로 내 커스텀 복사본 생성 후 추가")
    @PostMapping("/{ruleSetId}/sell-rules")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> addSellRule(
            @PathVariable Long ruleSetId,
            @RequestBody @Valid RuleCreateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.addSellRule(userProvider.getCurrentUserId(), ruleSetId, request)));
    }

    @Operation(summary = "원칙 수정", description = "룰셋 내 특정 원칙 내용 수정")
    @PatchMapping("/{ruleSetId}/rules/{ruleId}")
    public ResponseEntity<ApiResponse<RuleSetResDTO>> updateRule(
            @PathVariable Long ruleSetId,
            @PathVariable Long ruleId,
            @RequestBody @Valid RuleUpdateReqDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleSetService.updateRule(userProvider.getCurrentUserId(), ruleSetId, ruleId, request)));
    }

    @Operation(summary = "원칙 삭제", description = "룰셋 내 특정 원칙 삭제 (소프트 삭제)")
    @DeleteMapping("/{ruleSetId}/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long ruleSetId,
            @PathVariable Long ruleId) {
        ruleSetService.deleteRule(userProvider.getCurrentUserId(), ruleSetId, ruleId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
