package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long fromId;

    private Long toId;

    /** 评分：1-5 */
    private Integer score;

    private String content;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
