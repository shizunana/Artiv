package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminProjectModerationRequest {
    /** HIDE 或 RESTORE */
    @NotBlank
    private String action;

    @NotBlank(message = "审核原因不能为空")
    @Size(max = 500)
    private String reason;
}
