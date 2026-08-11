package com.huiyue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码仅用于存储与校验，序列化时忽略 */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    /** 角色：1 企划方、2 画师、3 管理员 */
    private Integer role;

    /** 状态：0 正常、1 封禁 */
    private Integer status;

    private String intro;

    /** 逻辑删除：0 未删、1 已删 */
    @JsonIgnore
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
