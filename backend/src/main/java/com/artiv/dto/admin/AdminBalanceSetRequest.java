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
public class AdminBalanceSetRequest {
    @NotNull(message = "请输入调整后余额")
    @DecimalMin(value = "0.00", message = "调整后余额不能小于 0")
    @DecimalMax(value = "9999999999.99", message = "调整后余额超过账户上限")
    @Digits(integer = 10, fraction = 2, message = "余额最多保留两位小数")
    private BigDecimal balance;

    @NotBlank(message = "请填写调整原因")
    @Size(max = 500, message = "调整原因最多 500 字")
    private String reason;
}
