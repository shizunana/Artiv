package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.Application;
import com.huiyue.mapper.ApplicationMapper;
import com.huiyue.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;

    @Override
    public Page<Application> listByProject(long page, long size, Long projectId) {
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .eq(Application::getProjectId, projectId)
                .orderByDesc(Application::getCreatedAt);
        return applicationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Application> listMine(long page, long size, Long painterId) {
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .eq(Application::getPainterId, painterId)
                .orderByDesc(Application::getCreatedAt);
        return applicationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Application create(Application application) {
        application.setId(null);
        application.setStatus(0);
        application.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationMapper.insert(application);
        return application;
    }

    @Override
    public void withdraw(Long id, Long painterId) {
        Application application = applicationMapper.selectById(id);
        if (application == null || !application.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (application.getStatus() != 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
        }
        application.setStatus(3);
        applicationMapper.updateById(application);
    }
}
