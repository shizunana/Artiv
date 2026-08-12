package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.dto.ProjectRequest;
import com.huiyue.entity.Project;

import java.math.BigDecimal;

public interface ProjectService {

    Page<Project> listProjects(long page, long size, String keyword, Integer status,
                               String sort, BigDecimal budgetMin, BigDecimal budgetMax,
                               Integer deadlineWithin, Integer deadlineAfter, String tag);

    Page<Project> listMine(long page, long size, Long employerId);

    Project create(ProjectRequest request, Long employerId);

    Project getById(Long id);
}
