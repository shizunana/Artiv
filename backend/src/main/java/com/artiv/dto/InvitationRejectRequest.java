package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvitationRejectRequest {

    @NotBlank(message = "请填写婉拒理由")
    @Size(max = 500, message = "婉拒理由不能超过 500 字")
    private String reason;
}
