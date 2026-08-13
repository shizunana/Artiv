package com.huiyue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StallRequest {

    @NotBlank(message = "服务名称不能为空")
    private String title;

    @NotBlank(message = "请选择橱窗类型")
    private String category;

    @NotBlank(message = "必须上传封面图")
    private String cover;

    @NotBlank(message = "必须上传至少一张橱窗详情图")
    private String images;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    @NotBlank(message = "截稿时间不能为空")
    private String deliverTime;

    @NotBlank(message = "创作阶段不能为空")
    private String phases;

    @NotBlank(message = "稿件参数不能为空")
    private String params;

    @NotBlank(message = "橱窗详情文字描述不能为空")
    private String description;
}
