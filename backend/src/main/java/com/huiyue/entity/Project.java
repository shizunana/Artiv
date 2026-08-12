package com.huiyue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private String images;

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
