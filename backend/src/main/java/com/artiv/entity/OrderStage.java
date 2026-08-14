package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单阶段实体
 */
@Data
@TableName("order_stage")
public class OrderStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String name;

    private Integer stageIndex;

    private Integer submissionNo;

    /** 阶段占比（%） */
    private Integer percent;

    /** 提交方：1 画师、2 企划方 */
    private Integer submitter;

    private String content;

    private String attachments;

    /** 高清原图地址，只有最终确认收货后才向企划方返回。 */
    private String originals;

    private String reviewRemark;

    private LocalDateTime reviewedAt;

    /** 状态：0 待确认、1 已通过、2 需修改 */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
