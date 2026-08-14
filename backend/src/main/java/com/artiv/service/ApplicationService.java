package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.ApplicationDetail;
import com.artiv.entity.Application;

public interface ApplicationService {

    Page<ApplicationDetail> listApplicantDetails(long page, long size, Long projectId);

    Page<Application> listMine(long page, long size, Long painterId);

    Application create(Application application);

    Application select(Long id, Long employerId);

    Application updateQuote(Long id, Long painterId, java.math.BigDecimal quote);

    void withdraw(Long id, Long painterId);
}
