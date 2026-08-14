package com.artiv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "被评价用户不能为空")
    private Long toId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为 1-5")
    @Max(value = 5, message = "评分范围为 1-5")
    private Integer score;

    private String content;
}
