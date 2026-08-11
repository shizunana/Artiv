package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Application;

public interface ApplicationService {

    Page<Application> listByProject(long page, long size, Long projectId);

    Page<Application> listMine(long page, long size, Long painterId);

    Application create(Application application);

    void withdraw(Long id, Long painterId);
}
