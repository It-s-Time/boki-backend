package com.boki.backend.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReviewSaveRequest(
        @Schema(description = "복기에 사용할 룰셋 ID", example = "1")
        @NotNull(message = "룰셋 ID는 필수입니다.")
        Long ruleSetId,

        @Valid
        @ArraySchema(
                arraySchema = @Schema(
                        description = "거래 타입에 맞는 활성 원칙 전체 점수 목록",
                        example = """
                                [
                                  { "ruleId": 1, "score": 5 },
                                  { "ruleId": 2, "score": 3 },
                                  { "ruleId": 3, "score": 4 }
                                ]
                                """
                ),
                schema = @Schema(implementation = ReviewScoreRequest.class)
        )
        @NotEmpty(message = "원칙 점수는 하나 이상 필요합니다.")
        List<ReviewScoreRequest> scores,

        @Schema(description = "복기 메모", example = "복기 내용")
        @Size(max = 5000, message = "복기 내용은 5000자 이하여야 합니다.")
        String content,

        @Schema(description = "수정 시 기존 이미지 교체 여부", example = "false")
        Boolean replaceImages
) {
    public boolean shouldReplaceImages() {
        return Boolean.TRUE.equals(replaceImages);
    }
}
