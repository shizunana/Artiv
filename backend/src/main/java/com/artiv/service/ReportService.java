package com.artiv.service;

import com.artiv.dto.ReportCreateRequest;
import com.artiv.dto.ReportSubmitResponse;
import com.artiv.dto.admin.AdminReportCaseItem;
import com.artiv.dto.admin.AdminReportDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;

public interface ReportService {
    ReportSubmitResponse submit(Long reporterId, ReportCreateRequest request);
    Page<AdminReportCaseItem> listAdmin(long page, long size, Integer status, String keyword,
                                        LocalDate startDate, LocalDate endDate);
    AdminReportDetail getAdminDetail(Long caseId);
    AdminReportDetail resolve(Long caseId, Long adminId, boolean handled, String reason);
    long countPending();
}
