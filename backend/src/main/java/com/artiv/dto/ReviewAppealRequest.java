package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewAppealRequest {
    @NotBlank(message = "申诉原因不能为空")
    @Size(max = 500, message = "申诉原因不能超过500字")
    private String reason;
}
