package com.boki.backend.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ReviewMultipartRequest", description = "복기 저장 multipart/form-data 요청")
public record ReviewMultipartRequest(
        @Schema(
                description = "복기 저장 JSON",
                implementation = ReviewSaveRequest.class,
                example = """
                        {
                          "ruleSetId": 1,
                          "scores": [
                            { "ruleId": 1, "score": 5 },
                            { "ruleId": 2, "score": 3 },
                            { "ruleId": 3, "score": 4 }
                          ],
                          "content": "복기 내용",
                          "replaceImages": false
                        }
                        """
        )
        ReviewSaveRequest request,

        @ArraySchema(
                schema = @Schema(type = "string", format = "binary", description = "복기 이미지 파일")
        )
        List<String> images
) {
}
