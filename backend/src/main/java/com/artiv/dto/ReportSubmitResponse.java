package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReportSubmitResponse {
    private Long caseId;
    private boolean recorded;
    private long reporterCount;
}
