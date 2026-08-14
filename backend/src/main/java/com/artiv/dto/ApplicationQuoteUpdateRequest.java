package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplicationQuoteUpdateRequest {

    @NotNull(message = "请填写报价")
    @DecimalMin(value = "0.01", message = "报价必须大于 0")
    private BigDecimal quote;
}
