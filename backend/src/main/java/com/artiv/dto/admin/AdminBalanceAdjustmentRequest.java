package com.artiv.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminBalanceAdjustmentRequest {
    @NotNull(message = "请输入调整金额")
    @DecimalMin(value = "0.01", message = "调整金额至少为 0.01")
    @DecimalMax(value = "10000000.00", message = "单次调整金额不能超过 10000000.00")
    @Digits(integer = 10, fraction = 2, message = "金额最多保留两位小数")
    private BigDecimal amount;

    @NotBlank(message = "请填写调整原因")
    @Size(max = 500, message = "调整原因最多 500 字")
    private String reason;
}
