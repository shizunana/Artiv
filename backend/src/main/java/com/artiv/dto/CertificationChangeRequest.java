package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 已认证用户提交的正式认证资料修改申请。 */
@Data
public class CertificationChangeRequest {
    @NotBlank(message = "笔名不能为空")
    @Size(max = 100)
    private String commonId;

    @NotBlank(message = "常用站不能为空")
    @Size(max = 500)
    private String commonSite;
}
