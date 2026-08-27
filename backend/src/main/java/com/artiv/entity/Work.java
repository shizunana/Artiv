package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品表实体
 */
@Data
@TableName("work")
public class Work {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String category;

    /** 题材风格，多个值使用英文逗号分隔 */
    private String subject;

    private String image;

    private String description;

    /** 可见范围：0 仅自己可见、1 公开 */
    private Integer visibility;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 公开列表展示字段，不落库。 */
    @TableField(exist = false)
    private Long favoriteCount;

    /** 当前登录用户是否已收藏，不落库。 */
    @TableField(exist = false)
    private Boolean favorited;
}
