package com.artiv.service;

import com.artiv.dto.FollowStateResponse;
import com.artiv.dto.ArtistSummary;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FollowService {
    FollowStateResponse followPainter(Long painterId, Long userId);

    FollowStateResponse unfollowPainter(Long painterId, Long userId);

    FollowStateResponse getPainterState(Long painterId, Long userId);

    Page<ArtistSummary> listPainters(long page, long size, Long userId, String subjects, String keyword);
}
