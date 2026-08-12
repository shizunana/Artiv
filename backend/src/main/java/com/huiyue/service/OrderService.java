package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.OrderInfo;

public interface OrderService {

    Page<OrderInfo> listMine(long page, long size, Long userId);

    OrderInfo getById(Long id);

    OrderInfo create(OrderInfo order);

    OrderInfo createStallPurchase(Long stallId, Long employerId);
}
