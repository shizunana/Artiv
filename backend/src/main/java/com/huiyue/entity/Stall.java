package com.huiyue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容服务板实体
 */
@Data
@TableName("stall")
public class Stall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long painterId;

    private String title;

    private String category;

    /** 封面图 */
    private String cover;

    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 截稿时间 / 交付周期 */
    private String deliverTime;

    /** 创作阶段 JSON：[{"name":"初稿","percent":40},...] */
    private String phases;

    /** 稿件参数 JSON */
    private String params;

    private String description;

    /** 状态：0 已下架、1 上架中 */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
