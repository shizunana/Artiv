package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企划完整内容编辑入参。
 *
 * <p>刻意不包含合作方式、邀请目标、截稿日期和业务状态，避免内容编辑越过
 * “变更企划”的既有状态规则。</p>
 */
@Data
public class ProjectContentUpdateRequest {

    @NotBlank(message = "企划标题不能为空")
    @Size(max = 20, message = "企划标题最长 20 个字")
    private String title;

    @NotBlank(message = "需求描述不能为空")
    @Size(max = 1000, message = "需求描述最长 1000 个字")
    private String description;

    @DecimalMin(value = "0.01", message = "最低预算必须大于 0")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.01", message = "最高预算必须大于 0")
    private BigDecimal budgetMax;

    private List<String> tags;

    private String category;

    private List<String> subject;

    private String images;

    private ProjectRequirements requirements;

    private String phases;
}
