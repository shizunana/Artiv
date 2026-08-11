package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Project;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    @Override
    public Page<Project> listProjects(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .eq(status != null, Project::getStatus, status)
                .like(StringUtils.hasText(keyword), Project::getTitle, keyword)
                .orderByDesc(Project::getCreatedAt);
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
