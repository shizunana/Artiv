package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_dispute_evidence")
public class OrderDisputeEvidence {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long disputeId;
    private Long submittedBy;
    private String image;
    private String description;
    private LocalDateTime createdAt;
}
