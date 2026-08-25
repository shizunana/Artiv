package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定向邀请实体
 */
@Data
@TableName("invitation")
public class Invitation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long painterId;

    private String message;

    /** 类型：0 定向邀请、1 公开企划邀请报价、2 公开企划带价邀请 */
    private Integer invitationType;

    /** 带价邀请的企划方预设价格 */
    private BigDecimal offeredPrice;

    /** 状态：0 待处理、1 已接受、2 已拒绝、3 已失效 */
    private Integer status;

    /** 接受邀请后创建或恢复的普通应征记录 */
    private Long applicationId;

    private String rejectReason;

    private LocalDateTime respondedAt;

    /** 画师是否已从“收到的邀请”中隐藏该提醒：0 否、1 是 */
    private Integer painterHidden;

    private LocalDateTime painterHiddenAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
