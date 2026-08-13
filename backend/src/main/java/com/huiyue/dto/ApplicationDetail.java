package com.huiyue.dto;

import com.huiyue.entity.Application;
import com.huiyue.entity.Work;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationDetail {

    private Application application;

    private ApplicantPainterSummary painter;

    private List<Work> works;
}
