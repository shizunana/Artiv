package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.artiv.dto.PublisherSummary;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企划表实体
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employerId;

    private String title;

    private String description;

    private BigDecimal budget;

    /** 预算区间下限（元）。 */
    private BigDecimal budgetMin;

    /** 预算区间上限（元）；legacy budget 同步保存该值。 */
    private BigDecimal budgetMax;

    private LocalDate deadline;

    private String tags;

    /** 企划类型（独立六类） */
    private String projectCategory;

    /** 题材风格，逗号分隔 */
    private String subject;

    private String images;

    private String requirements;

    /** 创作阶段 JSON：[{"name":"初稿"},{"name":"修改"},{"name":"终稿"}] */
    private String phases;

    @TableField(exist = false)
    private PublisherSummary employer;

    /** 合作方式：0 公开招募、1 定向邀请 */
    private Integer mode;

    /** 定向邀请状态，仅用于企划详情和本人企划列表，不落 project 表。 */
    @TableField(exist = false)
    private Integer invitationStatus;

    /** 当前发布者是否仍可完整编辑企划内容，仅面板接口返回。 */
    @TableField(exist = false)
    private Boolean contentEditable;

    /** 当前企划是否允许删除，仅在企划方列表接口中动态计算。 */
    @TableField(exist = false)
    private Boolean deletable;

    /** 无法删除时的业务原因，仅用于前端展示。 */
    @TableField(exist = false)
    private String deleteBlockReason;

    /** 状态：0 招募中、1 已选定、2 进行中、3 已完成、4 已下架 */
    private Integer status;

    /** 管理审核：0 正常、1 违规下架。与业务状态分离。 */
    private Integer moderationStatus;

    private String moderationReason;

    private Long moderatedBy;

    private LocalDateTime moderatedAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
