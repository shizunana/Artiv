package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CertificationSubmitRequest {
    @NotBlank(message = "笔名不能为空")
    @Size(max = 100)
    private String commonId;

    @NotBlank(message = "常用站不能为空")
    @Size(max = 500)
    private String commonSite;
}
