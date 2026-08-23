package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
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

    /** 账户可用余额；充值、提现和冻结金额将在正式资金模块中实现。 */
    private BigDecimal balance;

    /** 角色：1 企划方、2 画师、3 管理员 */
    private Integer role;

    /** 画师认证：0 未认证、1 审核中、2 已认证 */
    private Integer certStatus;

    /** 状态：0 正常、1 封禁 */
    private Integer status;

    private String banReason;

    private Long bannedBy;

    private LocalDateTime bannedAt;

    /** 限时封禁截止时间；永久封禁为空。 */
    private LocalDateTime bannedUntil;

    private String bio;

    /** 企划方公开主页简介；与画师简介 bio 分开维护。 */
    private String employerBio;

    /** 风格标签，逗号分隔 */
    private String tags;

    /** 当前已审核生效的画师笔名。 */
    private String artistName;

    /** 当前已审核生效的画师常用站。 */
    private String artistSite;

    /** 逻辑删除：0 未删、1 已删 */
    @JsonIgnore
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
