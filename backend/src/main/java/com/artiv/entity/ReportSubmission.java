package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_submission")
public class ReportSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long caseId;
    private Long reporterId;
    private String category;
    private String reason;
    private LocalDateTime createdAt;
}
