package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.dto.ArtistCategoryAggregate;
import com.artiv.entity.Work;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WorkMapper extends BaseMapper<Work> {

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
