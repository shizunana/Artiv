package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminArbitrationRequest {
    /** REFUND_EMPLOYER 或 SETTLE_PAINTER；PARTIAL_REFUND 仅保留接口值并明确拒绝。 */
    @NotBlank
    private String result;

    @NotBlank(message = "仲裁说明不能为空")
    @Size(max = 500)
    private String reason;
}
