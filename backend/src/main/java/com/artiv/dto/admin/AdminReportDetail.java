package com.artiv.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminReportDetail extends AdminReportCaseItem {
    private List<AdminReportSubmissionItem> submissions;
}
