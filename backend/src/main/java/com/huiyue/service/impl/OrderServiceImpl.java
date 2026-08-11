package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.OrderInfo;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;

    @Override
    public Page<OrderInfo> listMine(long page, long size, Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0)
                .and(w -> w.eq(OrderInfo::getEmployerId, userId).or().eq(OrderInfo::getPainterId, userId))
                .orderByDesc(OrderInfo::getCreatedAt);
        return orderInfoMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public OrderInfo getById(Long id) {
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return order;
    }

    @Override
    public OrderInfo create(OrderInfo order) {
        order.setId(null);
        order.setStatus(0);
        order.setCurrentStage(0);
        order.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderInfoMapper.insert(order);
        return order;
    }
}
