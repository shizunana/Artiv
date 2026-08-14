package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.WorkDetailResponse;
import com.artiv.entity.Work;
import com.artiv.entity.User;
import com.artiv.mapper.WorkMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;
    private final UserMapper userMapper;

    @Override
    public Page<Work> listWorks(long page, long size, String category, String subject, String keyword, String sort) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<Work>()
                .eq(Work::getDeleted, 0)
                .eq(Work::getVisibility, 1)
                .eq(StringUtils.hasText(category), Work::getCategory, category)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Work::getTitle, keyword)
                        .or()
                        .like(Work::getDescription, keyword));

        if (StringUtils.hasText(subject)) {
            for (String item : subject.split(",")) {
                if (StringUtils.hasText(item)) {
                    wrapper.like(Work::getSubject, item.trim());
                }
            }
        }
        if ("oldest".equals(sort)) {
            wrapper.orderByAsc(Work::getCreatedAt);
        } else {
            wrapper.orderByDesc(Work::getCreatedAt);
        }
        return workMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public WorkDetailResponse getPublicDetail(Long id) {
        Work work = workMapper.selectOne(new LambdaQueryWrapper<Work>()
                .eq(Work::getId, id)
                .eq(Work::getDeleted, 0)
                .eq(Work::getVisibility, 1));
        if (work == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        User author = userMapper.selectById(work.getUserId());
        if (author == null || Integer.valueOf(1).equals(author.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }

        WorkDetailResponse response = new WorkDetailResponse();
        BeanUtils.copyProperties(work, response);
        response.setAuthorName(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
        response.setAuthorAvatar(author.getAvatar());
        response.setAuthorBio(author.getBio());
        response.setAuthorCertStatus(author.getCertStatus());
        return response;
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
