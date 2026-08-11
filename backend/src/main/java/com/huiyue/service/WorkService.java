package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Work;

public interface WorkService {

    Page<Work> listWorks(long page, long size, String category);

    Page<Work> listMine(long page, long size, Long userId);

    Work create(Work work, Long userId);

    void delete(Long id, Long userId);
}
