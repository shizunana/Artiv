package com.artiv.mapper;

import com.artiv.dto.FavoriteStallItem;
import com.artiv.entity.StallFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StallFavoriteMapper extends BaseMapper<StallFavorite> {

    @Select("""
            SELECT s.id AS stall_id,
                   s.painter_id,
                   s.title,
                   s.category,
                   s.cover,
                   s.price,
                   s.stock,
                   s.deliver_time,
                   u.nickname AS painter_nickname,
                   u.avatar AS painter_avatar,
                   f.created_at AS favorited_at
            FROM stall_favorite f
            JOIN stall s ON s.id = f.stall_id
            JOIN user u ON u.id = s.painter_id
            WHERE f.user_id = #{userId}
              AND s.deleted = 0
              AND s.status = 1
              AND u.deleted = 0
              AND u.status = 0
              AND u.cert_status = 2
            ORDER BY f.created_at DESC, f.id DESC
            """)
    Page<FavoriteStallItem> selectMine(Page<FavoriteStallItem> page, @Param("userId") Long userId);
}
