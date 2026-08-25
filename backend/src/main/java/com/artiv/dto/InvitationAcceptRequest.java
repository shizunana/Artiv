package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvitationAcceptRequest {

    @DecimalMin(value = "0.01", message = "报价必须大于 0")
    private BigDecimal quote;

    @Size(max = 500, message = "应征说明不能超过 500 字")
    private String intro;
}
