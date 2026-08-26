package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportDecisionRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
