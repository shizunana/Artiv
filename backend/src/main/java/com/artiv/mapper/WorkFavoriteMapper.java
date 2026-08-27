package com.artiv.mapper;

import com.artiv.dto.FavoriteWorkItem;
import com.artiv.dto.WorkFavoriteCount;
import com.artiv.entity.WorkFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WorkFavoriteMapper extends BaseMapper<WorkFavorite> {

    @Select("""
            <script>
            SELECT w.id AS work_id,
                   w.user_id,
                   w.title,
                   w.category,
                   w.subject,
                   w.image,
                   w.description,
                   u.nickname AS author_nickname,
                   u.avatar AS author_avatar,
                   f.created_at AS favorited_at,
                   COALESCE(favorite_summary.favorite_count, 0) AS favorite_count
            FROM work_favorite f
            JOIN work w ON w.id = f.work_id
            JOIN user u ON u.id = w.user_id
            LEFT JOIN (
                SELECT work_id, COUNT(*) AS favorite_count
                FROM work_favorite
                GROUP BY work_id
            ) favorite_summary ON favorite_summary.work_id = w.id
            WHERE f.user_id = #{userId}
              AND w.deleted = 0
              AND w.visibility = 1
              AND u.deleted = 0
              AND u.status = 0
            <if test="category != null and category != ''">
              AND w.category = #{category}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (w.title LIKE CONCAT('%', #{keyword}, '%')
                OR w.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <foreach collection="subjects" item="subject">
              AND w.subject LIKE CONCAT('%', #{subject}, '%')
            </foreach>
            ORDER BY f.created_at DESC, f.id DESC
            </script>
            """)
    Page<FavoriteWorkItem> selectMine(Page<FavoriteWorkItem> page,
                                      @Param("userId") Long userId,
                                      @Param("category") String category,
                                      @Param("subjects") java.util.List<String> subjects,
                                      @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT work_id, COUNT(*) AS favorite_count
            FROM work_favorite
            WHERE work_id IN
            <foreach collection="workIds" item="workId" open="(" separator="," close=")">
                #{workId}
            </foreach>
            GROUP BY work_id
            </script>
            """)
    List<WorkFavoriteCount> selectCounts(@Param("workIds") List<Long> workIds);

    @Select("""
            <script>
            SELECT work_id
            FROM work_favorite
            WHERE user_id = #{userId}
              AND work_id IN
            <foreach collection="workIds" item="workId" open="(" separator="," close=")">
                #{workId}
            </foreach>
            </script>
            """)
    List<Long> selectFavoritedWorkIds(@Param("userId") Long userId,
                                      @Param("workIds") List<Long> workIds);
}
