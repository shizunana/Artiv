package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.artiv.dto.OrderParticipantSummary;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（表名 order_info，order 为 MySQL 保留字）
 */
@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 企划订单对应的应征记录；允许同一企划给多名应征画师分别发单。 */
    private Long applicationId;

    private Long stallId;

    private Long employerId;

    private Long painterId;

    /** 企划方订单列表按画师分组时使用，不落库。 */
    @TableField(exist = false)
    private OrderParticipantSummary painter;

    private BigDecimal amount;

    /** 下单时的橱窗标题快照，避免橱窗后续修改影响历史订单。 */
    private String stallTitle;

    private String stallCover;

    /** 下单时的企划信息快照，避免企划后续变更影响历史订单。 */
    private String projectTitle;

    private String projectCover;

    private String projectSnapshot;

    /** 下单时复制的创作阶段 JSON。 */
    private String phaseSnapshot;

    /** 状态：0 待接单、1 进行中、2 待验收、3 已完成、4 已取消、5 争议中 */
    private Integer status;

    private Integer currentStage;

    private String cancelReason;

    private String afterSaleReason;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
