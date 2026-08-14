package com.artiv.dto;

import com.artiv.entity.Application;
import com.artiv.entity.Work;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationDetail {

    private Application application;

    private ApplicantPainterSummary painter;

    private List<Work> works;

    private Boolean hasActiveOrder;

    private Long activeOrderId;
}
