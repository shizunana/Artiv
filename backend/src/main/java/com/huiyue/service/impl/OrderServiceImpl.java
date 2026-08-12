package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.mapper.StallMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.MessageService;
import com.huiyue.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final StallMapper stallMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

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

    @Override
    @Transactional
    public OrderInfo createStallPurchase(Long stallId, Long employerId) {
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null || !Integer.valueOf(0).equals(stall.getDeleted())
                || !Integer.valueOf(1).equals(stall.getStatus())
                || stall.getStock() == null || stall.getStock() <= 0
                || employerId.equals(stall.getPainterId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        User painter = userMapper.selectById(stall.getPainterId());
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        OrderInfo order = new OrderInfo();
        order.setStallId(stallId);
        order.setEmployerId(employerId);
        order.setPainterId(stall.getPainterId());
        order.setAmount(stall.getPrice());
        create(order);
        messageService.createStallPurchaseCommunication(order, stall);
        return order;
    }
}
