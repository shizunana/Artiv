package com.artiv.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MyApplicationItem {

    private Long id;

    private Long projectId;

    private String projectTitle;

    private String projectImages;

    private String projectCategory;

    private String projectDescription;

    private String intro;

    private String workLink;

    private BigDecimal quote;

    private LocalDateTime createdAt;
}
