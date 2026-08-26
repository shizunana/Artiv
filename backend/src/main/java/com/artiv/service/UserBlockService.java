package com.artiv.service;

import com.artiv.dto.BlockStatusResponse;
import com.artiv.dto.BlockedUserItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserBlockService {
    Page<BlockedUserItem> listMine(long page, long size, Long blockerId);
    BlockStatusResponse getStatus(Long blockerId, Long targetId);
    BlockStatusResponse block(Long blockerId, Long targetId);
    BlockStatusResponse unblock(Long blockerId, Long targetId);
    boolean isEitherBlocked(Long firstUserId, Long secondUserId);
    void requireNotBlocked(Long firstUserId, Long secondUserId, String action);
}
