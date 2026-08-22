package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewAppealDecisionRequest {
    @NotBlank(message = "处理说明不能为空")
    @Size(max = 500, message = "处理说明不能超过500字")
    private String reason;
}
