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

    /** 状态：0 待处理、1 已通过、2 已拒绝、3 已撤销 */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
