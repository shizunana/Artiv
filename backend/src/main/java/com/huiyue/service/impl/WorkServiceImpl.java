package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.Work;
import com.huiyue.mapper.WorkMapper;
import com.huiyue.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;

    @Override
    public Page<Work> listWorks(long page, long size, String category, String subject, String keyword, String sort) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<Work>()
                .eq(Work::getDeleted, 0)
                .eq(Work::getVisibility, 1)
                .eq(StringUtils.hasText(category), Work::getCategory, category)
                .like(StringUtils.hasText(subject), Work::getSubject, subject)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Work::getTitle, keyword)
                        .or()
                        .like(Work::getDescription, keyword));
        if ("oldest".equals(sort)) {
            wrapper.orderByAsc(Work::getCreatedAt);
        } else {
            wrapper.orderByDesc(Work::getCreatedAt);
        }
        return workMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Work> listMine(long page, long size, Long userId) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<Work>()
                .eq(Work::getDeleted, 0)
                .eq(Work::getUserId, userId)
                .orderByDesc(Work::getCreatedAt);
        return workMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Work create(Work work, Long userId) {
        work.setId(null);
        work.setUserId(userId);
        work.setDeleted(0);
        if (work.getVisibility() == null) {
            work.setVisibility(1);
        }
        LocalDateTime now = LocalDateTime.now();
        work.setCreatedAt(now);
        work.setUpdatedAt(now);
        workMapper.insert(work);
        return work;
    }

    @Override
    public Work changeVisibility(Long id, Integer visibility, Long userId) {
        if (visibility == null || (visibility != 0 && visibility != 1)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        Work work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        work.setVisibility(visibility);
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(work);
        return work;
    }

    @Override
    public void delete(Long id, Long userId) {
        Work work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        workMapper.deleteById(id);
    }
}
