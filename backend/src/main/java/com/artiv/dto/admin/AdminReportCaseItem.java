package com.artiv.dto.admin;

import com.artiv.entity.ReportCase;
import lombok.Data;

@Data
public class AdminReportCaseItem {
    private ReportCase reportCase;
    private String targetNickname;
    private String targetAvatar;
    private Integer targetStatus;
    private long reporterCount;
}
