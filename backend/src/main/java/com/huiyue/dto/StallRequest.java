package com.huiyue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StallRequest {

    @NotBlank(message = "服务名称不能为空")
    private String title;

    private String category;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    private String description;
}
