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

    private LocalDate deadline;

    private String tags;

    /** 类型（同橱窗分类） */
    private String category;

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

    /** 招募截止时间 */
    private LocalDateTime recruitDeadline;

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
