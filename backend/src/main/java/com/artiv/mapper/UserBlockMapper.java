package com.artiv.mapper;

import com.artiv.dto.BlockedUserItem;
import com.artiv.entity.UserBlock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserBlockMapper extends BaseMapper<UserBlock> {
    @Select("""
            SELECT ub.blocked_id AS userId,
                   CASE WHEN u.id IS NULL OR u.deleted = 1 THEN '已注销用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS nickname,
                   CASE WHEN u.deleted = 1 THEN NULL ELSE u.avatar END AS avatar,
                   u.role AS role, u.cert_status AS certStatus,
                   COALESCE(u.deleted, 1) AS deleted, ub.created_at AS blockedAt
            FROM user_block ub
            LEFT JOIN `user` u ON u.id = ub.blocked_id
            WHERE ub.blocker_id = #{blockerId}
            ORDER BY ub.created_at DESC, ub.id DESC
            """)
    Page<BlockedUserItem> selectBlockedUsers(Page<BlockedUserItem> page,
                                              @Param("blockerId") Long blockerId);
}
