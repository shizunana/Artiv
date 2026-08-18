package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.PublicProfileResponse;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.Work;

public interface PublicProfileService {

    PublicProfileResponse getProfile(Long userId, String role);

    Page<Work> listWorks(Long userId, long page, long size);

    Page<Stall> listStalls(Long userId, long page, long size);

    Page<Project> listProjects(Long userId, long page, long size);
}
