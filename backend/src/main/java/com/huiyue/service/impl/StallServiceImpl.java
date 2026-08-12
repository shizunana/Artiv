package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.Stall;
import com.huiyue.mapper.StallMapper;
import com.huiyue.service.StallService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StallServiceImpl implements StallService {

    private final StallMapper stallMapper;

    @Override
    public Page<Stall> listStalls(long page, long size, String category, String keyword, String sort) {
        LambdaQueryWrapper<Stall> wrapper = new LambdaQueryWrapper<Stall>()
                .eq(Stall::getDeleted, 0)
                .eq(Stall::getStatus, 1)
                .eq(StringUtils.hasText(category), Stall::getCategory, category)
                .like(StringUtils.hasText(keyword), Stall::getTitle, keyword);
        if ("priceAsc".equals(sort)) {
            wrapper.orderByAsc(Stall::getPrice);
        } else if ("priceDesc".equals(sort)) {
            wrapper.orderByDesc(Stall::getPrice);
        } else {
            wrapper.orderByDesc(Stall::getCreatedAt);
        }
        return stallMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Stall> listMine(long page, long size, Long painterId) {
        LambdaQueryWrapper<Stall> wrapper = new LambdaQueryWrapper<Stall>()
                .eq(Stall::getDeleted, 0)
                .eq(Stall::getPainterId, painterId)
                .orderByDesc(Stall::getCreatedAt);
        return stallMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Stall getById(Long id) {
        Stall stall = stallMapper.selectById(id);
        if (stall == null || stall.getDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return stall;
    }

    @Override
    public Stall create(Stall stall, Long painterId) {
        stall.setId(null);
        stall.setPainterId(painterId);
        stall.setStatus(1);
        stall.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        stall.setCreatedAt(now);
        stall.setUpdatedAt(now);
        stallMapper.insert(stall);
        return stall;
    }

    @Override
    public Stall update(Long id, Stall stall, Long painterId) {
        Stall exist = getById(id);
        if (!exist.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        stall.setId(id);
        stall.setPainterId(painterId);
        stall.setUpdatedAt(LocalDateTime.now());
        stallMapper.updateById(stall);
        return getById(id);
    }

    @Override
    public Stall changeStatus(Long id, Integer status, Long painterId) {
        Stall exist = getById(id);
        if (!exist.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        exist.setStatus(status);
        stallMapper.updateById(exist);
        return exist;
    }
}
