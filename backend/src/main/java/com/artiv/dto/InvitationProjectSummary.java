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

    private String category;

    private BigDecimal budget;

    private Integer status;

    private String rejectReason;

    private Long applicationId;

    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}
