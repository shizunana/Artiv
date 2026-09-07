package com.artiv.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvitationProjectSummary {

    private Long invitationId;

    private Long projectId;

    private String cover;

    private String projectTitle;

    private String projectCategory;

    private String projectDescription;

    private BigDecimal budget;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;

    private Integer invitationType;

    private BigDecimal offeredPrice;

    private String message;

    private Integer status;

    private String rejectReason;

    private Long applicationId;

    private Boolean projectAvailable;

    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}
