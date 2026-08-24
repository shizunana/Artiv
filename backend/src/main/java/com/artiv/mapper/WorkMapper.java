package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.dto.ArtistCategoryAggregate;
import com.artiv.entity.Work;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WorkMapper extends BaseMapper<Work> {

    /** 作品广场“热门”排序：一次聚合收藏数，再按收藏数与发布时间稳定倒序。 */
    @Select("""
            <script>
            SELECT w.*
            FROM work w
            LEFT JOIN (
                SELECT work_id, COUNT(*) AS favorite_count
                FROM work_favorite
                GROUP BY work_id
            ) favorite_summary ON favorite_summary.work_id = w.id
            WHERE w.deleted = 0
              AND w.visibility = 1
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
            ORDER BY COALESCE(favorite_summary.favorite_count, 0) DESC,
                     w.created_at DESC,
                     w.id DESC
            </script>
            """)
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<Work> selectPopularPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Work> page,
            @Param("category") String category,
            @Param("subjects") List<String> subjects,
            @Param("keyword") String keyword);

    /**
     * 使用窗口函数只读取当前页每名画师最近的若干公开作品，避免把其全部作品载入内存。
     */
    @Select("""
            <script>
            SELECT id, user_id, title, category, subject, image, description,
                   visibility, deleted, created_at, updated_at
            FROM (
                SELECT w.*,
                       ROW_NUMBER() OVER (PARTITION BY w.user_id ORDER BY w.created_at DESC, w.id DESC) AS row_num
                FROM work w
                WHERE w.user_id IN
                <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                    #{userId}
                </foreach>
                  AND w.visibility = 1
                  AND w.deleted = 0
            ) ranked
            WHERE row_num &lt;= #{limit}
            ORDER BY user_id ASC, created_at DESC, id DESC
            </script>
            """)
    List<Work> selectPublicPreviews(@Param("userIds") List<Long> userIds,
                                    @Param("limit") int limit);

    /** 当前页画师的全部公开作品类型一次性聚合。 */
    @Select("""
            <script>
            SELECT user_id, GROUP_CONCAT(DISTINCT category ORDER BY category SEPARATOR ',') AS categories
            FROM work
            WHERE user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
              AND visibility = 1
              AND deleted = 0
              AND category IS NOT NULL
              AND category &lt;&gt; ''
            GROUP BY user_id
            </script>
            """)
    List<ArtistCategoryAggregate> selectPublicCategories(@Param("userIds") List<Long> userIds);
}
