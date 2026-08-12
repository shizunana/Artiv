package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.dto.ApplicationDetail;
import com.huiyue.entity.Application;

public interface ApplicationService {

    Page<Application> listByProject(long page, long size, Long projectId);

    Page<ApplicationDetail> listApplicantDetails(long page, long size, Long projectId, Long employerId);

    Page<Application> listMine(long page, long size, Long painterId);

    Application create(Application application);

    Application select(Long id, Long employerId);

    void withdraw(Long id, Long painterId);
}
