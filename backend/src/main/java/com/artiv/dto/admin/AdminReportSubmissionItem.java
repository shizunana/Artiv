package com.artiv.dto.admin;

import com.artiv.entity.ReportSubmission;
import lombok.Data;

import java.util.List;

@Data
public class AdminReportSubmissionItem {
    private ReportSubmission submission;
    private String reporterNickname;
    private String reporterAvatar;
    private List<String> images;
}
