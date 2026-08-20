package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_dispute")
public class OrderDispute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long initiatorId;
    private String reason;
    /** 0 待仲裁、1 已处理 */
    private Integer status;
    /** 1 退款企划方、2 结算画师、3 部分退款（预留未启用） */
    private Integer result;
    private String resolutionReason;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
