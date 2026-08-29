package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.ProjectRequest;
import com.artiv.dto.ProjectContentUpdateRequest;
import com.artiv.entity.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProjectService {

    Page<Project> listProjects(long page, long size, String keyword, Integer status,
                               String sort, BigDecimal budgetMin, BigDecimal budgetMax,
                               Integer deadlineWithin, Integer deadlineAfter, String tag,
                               String category, String subject);

    Page<Project> listMine(long page, long size, Long employerId, Integer mode);

    Project create(ProjectRequest request, Long employerId);

    Project getById(Long id, Long viewerId);

    Project getPanel(Long id, Long employerId);

    Project updateContent(Long id, ProjectContentUpdateRequest request, Long employerId);

    Project updateDeadline(Long id, LocalDate deadline, Long employerId);

    Project changeStatus(Long id, Integer status, LocalDate deadline, Long employerId);

    Project publishPublic(Long id, LocalDate deadline, Long employerId);

    void delete(Long id, Long employerId);
}
