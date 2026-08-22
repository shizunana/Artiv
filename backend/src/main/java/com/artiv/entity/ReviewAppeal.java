package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_appeal")
public class ReviewAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long applicantId;
    private String reason;
    /** 0 待处理、1 已批准、2 已驳回。 */
    private Integer status;
    private Long adminId;
    private String decisionReason;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
