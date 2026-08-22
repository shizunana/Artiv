package com.artiv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为 1-5")
    @Max(value = 5, message = "评分范围为 1-5")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    @Size(max = 6, message = "评价图片最多6张")
    private List<String> images;
}
