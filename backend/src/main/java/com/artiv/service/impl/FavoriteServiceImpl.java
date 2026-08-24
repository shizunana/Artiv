package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.FavoriteStallItem;
import com.artiv.dto.FavoriteStateResponse;
import com.artiv.dto.FavoriteWorkItem;
import com.artiv.entity.Stall;
import com.artiv.entity.StallFavorite;
import com.artiv.entity.Work;
import com.artiv.entity.WorkFavorite;
import com.artiv.mapper.StallFavoriteMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.WorkFavoriteMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.FavoriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final WorkFavoriteMapper workFavoriteMapper;
    private final StallFavoriteMapper stallFavoriteMapper;
    private final WorkMapper workMapper;
    private final StallMapper stallMapper;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    @Transactional
    public FavoriteStateResponse favoriteWork(Long workId, Long userId) {
        accountRestrictionService.requireAvailable(userId, "收藏作品");
        Work work = requirePublicWork(workId);
        if (work.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "不能收藏自己的作品");
        }
        if (!isWorkFavorited(workId, userId)) {
            WorkFavorite relation = new WorkFavorite();
            relation.setUserId(userId);
            relation.setWorkId(workId);
            relation.setCreatedAt(LocalDateTime.now());
            try {
                workFavoriteMapper.insert(relation);
            } catch (DuplicateKeyException ignored) {
                // 联合唯一索引处理并发重复点击，接口保持幂等。
            }
        }
        return new FavoriteStateResponse(workId, true);
    }

    @Override
    @Transactional
    public FavoriteStateResponse unfavoriteWork(Long workId, Long userId) {
        workFavoriteMapper.delete(new LambdaQueryWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .eq(WorkFavorite::getWorkId, workId));
        return new FavoriteStateResponse(workId, false);
    }

    @Override
    public FavoriteStateResponse getWorkState(Long workId, Long userId) {
        return new FavoriteStateResponse(workId, isWorkFavorited(workId, userId));
    }

    @Override
    public Page<FavoriteWorkItem> listWorks(long page, long size, Long userId) {
        return workFavoriteMapper.selectMine(new Page<>(safePage(page), safeSize(size)), userId);
    }

    @Override
    @Transactional
    public FavoriteStateResponse favoriteStall(Long stallId, Long userId) {
        accountRestrictionService.requireAvailable(userId, "收藏橱窗");
        Stall stall = requireListedStall(stallId);
        if (stall.getPainterId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "不能收藏自己的橱窗");
        }
        if (!isStallFavorited(stallId, userId)) {
            StallFavorite relation = new StallFavorite();
            relation.setUserId(userId);
            relation.setStallId(stallId);
            relation.setCreatedAt(LocalDateTime.now());
            try {
                stallFavoriteMapper.insert(relation);
            } catch (DuplicateKeyException ignored) {
                // 联合唯一索引处理并发重复点击，接口保持幂等。
            }
        }
        return new FavoriteStateResponse(stallId, true);
    }

    @Override
    @Transactional
    public FavoriteStateResponse unfavoriteStall(Long stallId, Long userId) {
        stallFavoriteMapper.delete(new LambdaQueryWrapper<StallFavorite>()
                .eq(StallFavorite::getUserId, userId)
                .eq(StallFavorite::getStallId, stallId));
        return new FavoriteStateResponse(stallId, false);
    }

    @Override
    public FavoriteStateResponse getStallState(Long stallId, Long userId) {
        return new FavoriteStateResponse(stallId, isStallFavorited(stallId, userId));
    }

    @Override
    public Page<FavoriteStallItem> listStalls(long page, long size, Long userId) {
        return stallFavoriteMapper.selectMine(new Page<>(safePage(page), safeSize(size)), userId);
    }

    private Work requirePublicWork(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work == null || Integer.valueOf(1).equals(work.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(work.getVisibility())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能收藏公开作品");
        }
        return work;
    }

    private Stall requireListedStall(Long stallId) {
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null || Integer.valueOf(1).equals(stall.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(stall.getStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能收藏上架中的橱窗");
        }
        return stall;
    }

    private boolean isWorkFavorited(Long workId, Long userId) {
        Long count = workFavoriteMapper.selectCount(new LambdaQueryWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .eq(WorkFavorite::getWorkId, workId));
        return count != null && count > 0;
    }

    private boolean isStallFavorited(Long stallId, Long userId) {
        Long count = stallFavoriteMapper.selectCount(new LambdaQueryWrapper<StallFavorite>()
                .eq(StallFavorite::getUserId, userId)
                .eq(StallFavorite::getStallId, stallId));
        return count != null && count > 0;
    }

    private long safePage(long page) {
        return Math.max(1, page);
    }

    private long safeSize(long size) {
        return Math.min(50, Math.max(1, size));
    }
}
