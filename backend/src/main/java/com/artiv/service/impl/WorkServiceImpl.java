package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.WorkDetailResponse;
import com.artiv.dto.WorkFavoriteCount;
import com.artiv.entity.Work;
import com.artiv.entity.WorkFavorite;
import com.artiv.entity.User;
import com.artiv.mapper.WorkMapper;
import com.artiv.mapper.WorkFavoriteMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;
    private final UserMapper userMapper;
    private final WorkFavoriteMapper workFavoriteMapper;

    @Override
    public Page<Work> listWorks(long page, long size, Long userId,
                                String category, String subject, String keyword, String sort) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        if ("popular".equals(sort)) {
            return enrichFavoriteState(workMapper.selectPopularPage(new Page<>(safePage, safeSize),
                    category, splitValues(subject), keyword), userId);
        }
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
        return enrichFavoriteState(workMapper.selectPage(new Page<>(safePage, safeSize), wrapper), userId);
    }

    @Override
    public Page<Work> listFollowingWorks(long page, long size, Long userId,
                                         String category, String subject, String keyword) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        return enrichFavoriteState(workMapper.selectFollowingPage(
                new Page<>(safePage, safeSize),
                userId,
                category,
                splitValues(subject),
                keyword
        ), userId);
    }

    private Page<Work> enrichFavoriteState(Page<Work> result, Long userId) {
        List<Long> workIds = result.getRecords().stream()
                .map(Work::getId)
                .distinct()
                .toList();
        if (workIds.isEmpty()) {
            return result;
        }
        Map<Long, Long> counts = workFavoriteMapper.selectCounts(workIds).stream()
                .collect(Collectors.toMap(
                        WorkFavoriteCount::getWorkId,
                        WorkFavoriteCount::getFavoriteCount,
                        (left, right) -> left));
        Set<Long> favoritedIds = userId == null
                ? Set.of()
                : new HashSet<>(workFavoriteMapper.selectFavoritedWorkIds(userId, workIds));
        result.getRecords().forEach(work -> {
            work.setFavoriteCount(counts.getOrDefault(work.getId(), 0L));
            work.setFavorited(favoritedIds.contains(work.getId()));
        });
        return result;
    }

    private List<String> splitValues(String values) {
        if (!StringUtils.hasText(values)) {
            return List.of();
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
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
        response.setFavoriteCount(workFavoriteMapper.selectCount(
                new LambdaQueryWrapper<WorkFavorite>()
                        .eq(WorkFavorite::getWorkId, id)));
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
