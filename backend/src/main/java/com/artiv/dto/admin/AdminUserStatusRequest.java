package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserStatusRequest {
    @NotNull
    private Integer status;

    @NotBlank(message = "操作原因不能为空")
    @Size(max = 500)
    private String reason;

    /** 仅封禁时使用；为空表示永久封禁。 */
    private LocalDateTime bannedUntil;
}
