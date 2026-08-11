package com.huiyue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private String image;

    private String description;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
