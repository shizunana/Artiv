package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReasonRequest {
    @NotBlank(message = "操作原因不能为空")
    @Size(max = 500)
    private String reason;
}
