package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_action_log")
public class AdminActionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String beforeData;
    private String afterData;
    private String reason;
    private String requestId;
    private String ipAddress;
    private LocalDateTime createdAt;
}
