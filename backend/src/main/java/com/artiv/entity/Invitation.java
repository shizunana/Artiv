package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    /** 状态：0 待处理、1 已接受、2 已拒绝 */
    private Integer status;

    /** 接受邀请后创建或恢复的普通应征记录 */
    private Long applicationId;

    private String rejectReason;

    private LocalDateTime respondedAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
