package com.artiv.service;

import com.artiv.dto.FavoriteStallItem;
import com.artiv.dto.FavoriteStateResponse;
import com.artiv.dto.FavoriteWorkItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FavoriteService {
    FavoriteStateResponse favoriteWork(Long workId, Long userId);

    FavoriteStateResponse unfavoriteWork(Long workId, Long userId);

    FavoriteStateResponse getWorkState(Long workId, Long userId);

    Page<FavoriteWorkItem> listWorks(long page, long size, Long userId);

    FavoriteStateResponse favoriteStall(Long stallId, Long userId);

    FavoriteStateResponse unfavoriteStall(Long stallId, Long userId);

    FavoriteStateResponse getStallState(Long stallId, Long userId);

    Page<FavoriteStallItem> listStalls(long page, long size, Long userId);
}
