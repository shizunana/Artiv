package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单操作时间线。 */
@Data
@TableName("order_event")
public class OrderEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long actorId;
    private String eventType;
    private String content;
    private LocalDateTime createdAt;
}
