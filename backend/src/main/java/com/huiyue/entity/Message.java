package com.huiyue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromId;

    private Long toId;

    private Long orderId;

    /** 类型：0 系统、1 应征、2 订单、3 私信 */
    private Integer type;

    private String content;

    /** 已读时间（NULL 表示未读） */
    private LocalDateTime readAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
