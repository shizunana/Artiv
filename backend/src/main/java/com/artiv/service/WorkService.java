package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.WorkDetailResponse;
import com.artiv.entity.Work;

public interface WorkService {

    Page<Work> listWorks(long page, long size, String category, String subject, String keyword, String sort);

    Page<Work> listFollowingWorks(long page, long size, Long userId,
                                  String category, String subject, String keyword);

    WorkDetailResponse getPublicDetail(Long id);

    Page<Work> listMine(long page, long size, Long userId);

    Work create(Work work, Long userId);

    Work changeVisibility(Long id, Integer visibility, Long userId);

    void delete(Long id, Long userId);
}
