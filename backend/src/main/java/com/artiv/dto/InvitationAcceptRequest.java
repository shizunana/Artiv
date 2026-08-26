package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvitationAcceptRequest {

    @DecimalMin(value = "0.01", message = "报价必须大于 0")
    private BigDecimal quote;
}
