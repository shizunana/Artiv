package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发布企划入参
 */
@Data
public class ProjectRequest {

    @NotBlank(message = "企划标题不能为空")
    @Size(max = 20, message = "企划标题最长 20 个字")
    private String title;

    @NotBlank(message = "需求描述不能为空")
    private String description;

    @NotNull(message = "预算不能为空")
    @DecimalMin(value = "0.01", message = "预算必须大于 0")
    private BigDecimal budget;

    @NotNull(message = "截稿日期不能为空")
    private LocalDate deadline;

    private List<String> tags;

    /** 类型（同橱窗分类） */
    private String category;

    /** 题材风格列表 */
    private List<String> subject;

    private String images;

    private ProjectRequirements requirements;

    /** 合作方式：0 公开招募、1 定向邀请 */
    private Integer mode;

    /** 定向邀请画师 ID（mode=1 时使用） */
    private Long invitePainterId;
}
