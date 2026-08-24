package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.ProjectRequest;
import com.artiv.entity.Project;

import java.math.BigDecimal;

public interface ProjectService {

    Page<Project> listProjects(long page, long size, String keyword, Integer status,
                               String sort, BigDecimal budgetMin, BigDecimal budgetMax,
                               Integer deadlineWithin, Integer deadlineAfter, String tag,
                               String category, String subject);

    Page<Project> listMine(long page, long size, Long employerId);

    Project create(ProjectRequest request, Long employerId);

    Project getById(Long id, Long viewerId);

    Project changeStatus(Long id, Integer status, Long employerId);

    Project publishPublic(Long id, Long employerId);

    void delete(Long id, Long employerId);
}
