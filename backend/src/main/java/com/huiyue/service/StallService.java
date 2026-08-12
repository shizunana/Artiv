package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Stall;

public interface StallService {

    Page<Stall> listStalls(long page, long size, String category, String keyword, String sort,
                           String subject, java.math.BigDecimal priceMin, java.math.BigDecimal priceMax,
                           String deliverTime);

    Page<Stall> listMine(long page, long size, Long painterId);

    Stall getById(Long id);

    Stall create(Stall stall, Long painterId);

    Stall update(Long id, Stall stall, Long painterId);

    Stall changeStatus(Long id, Integer status, Long painterId);

    void delete(Long id, Long painterId);
}
