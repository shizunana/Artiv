package com.artiv.mapper;

import com.artiv.dto.FavoriteWorkItem;
import com.artiv.entity.WorkFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WorkFavoriteMapper extends BaseMapper<WorkFavorite> {

    @Select("""
            SELECT w.id AS work_id,
                   w.user_id,
                   w.title,
                   w.category,
                   w.subject,
                   w.image,
                   w.description,
                   u.nickname AS author_nickname,
                   u.avatar AS author_avatar,
                   f.created_at AS favorited_at
            FROM work_favorite f
            JOIN work w ON w.id = f.work_id
            JOIN user u ON u.id = w.user_id
            WHERE f.user_id = #{userId}
              AND w.deleted = 0
              AND w.visibility = 1
              AND u.deleted = 0
              AND u.status = 0
            ORDER BY f.created_at DESC, f.id DESC
            """)
    Page<FavoriteWorkItem> selectMine(Page<FavoriteWorkItem> page, @Param("userId") Long userId);
}
