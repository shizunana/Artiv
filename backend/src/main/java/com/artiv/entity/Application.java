package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应征实体
 */
@Data
@TableName("application")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long painterId;

    private String intro;

    private String workLink;

    private BigDecimal quote;

    /** 报价来源：0 画师报价、1 企划方带价邀请（锁定不可修改） */
    private Integer quoteSource;

    /** 状态：0 待处理、1 已通过、2 已拒绝、3 已撤销 */
    private Integer status;

    /** 是否仍是企划当前有效应征关系：0 否、1 是。 */
    private Integer active;

    /** 应征关系终止原因：WITHDRAWN、REJECTED、ORDER_COMPLETED。 */
    private String endedReason;

    private LocalDateTime endedAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
