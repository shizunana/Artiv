package com.artiv.mapper;

import com.artiv.dto.FollowedPainterItem;
import com.artiv.entity.UserFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("""
            SELECT u.id AS painter_id,
                   u.nickname,
                   u.avatar,
                   u.bio,
                   u.tags,
                   f.created_at AS followed_at
            FROM user_follow f
            JOIN user u ON u.id = f.painter_id
            WHERE f.user_id = #{userId}
              AND u.deleted = 0
              AND u.status = 0
              AND u.cert_status = 2
            ORDER BY f.created_at DESC, f.id DESC
            """)
    Page<FollowedPainterItem> selectMine(Page<FollowedPainterItem> page, @Param("userId") Long userId);
}
