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

    @TableField(exist = false)
    private PublisherSummary employer;

    /** 合作方式：0 公开招募、1 定向邀请 */
    private Integer mode;

    /** 招募截止时间 */
    private LocalDateTime recruitDeadline;

    /** 状态：0 招募中、1 已选定、2 进行中、3 已完成、4 已关闭 */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
