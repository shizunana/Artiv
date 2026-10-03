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
public class ProjectRequest extends ProjectCategoryInput {

    @NotBlank(message = "企划标题不能为空")
    @Size(max = 20, message = "企划标题最长 20 个字")
    private String title;

    @NotBlank(message = "需求描述不能为空")
    private String description;

    @DecimalMin(value = "0.01", message = "预算必须大于 0")
    private BigDecimal budget;

    @DecimalMin(value = "0.01", message = "最低预算必须大于 0")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.01", message = "最高预算必须大于 0")
    private BigDecimal budgetMax;

    @NotNull(message = "截稿日期不能为空")
    private LocalDate deadline;

    private List<String> tags;

    /** 企划类型（独立六类） */
    @NotBlank(message = "请选择企划类型")
    @Size(max = 50, message = "企划类型最长 50 字")
    private String projectCategory;

    /** 稿件用途；旧客户端未传时兼容为个人用途。 */
    private String usageType;

    /** 题材风格列表 */
    private List<String> subject;

    private String images;

    private ProjectRequirements requirements;

    /** 创作阶段 JSON；服务端会忽略旧版 percent 字段并统一保存名称。 */
    private String phases;

    /** 合作方式：0 公开招募、1 定向邀请 */
    private Integer mode;

    /** 定向邀请画师 ID（mode=1 时使用） */
    private Long invitePainterId;
}
