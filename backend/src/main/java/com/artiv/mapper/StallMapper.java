package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Stall;

public interface StallMapper extends BaseMapper<Stall> {
    @org.apache.ibatis.annotations.Select("""
            <script>
            SELECT painter_id AS user_id, GROUP_CONCAT(DISTINCT category ORDER BY category SEPARATOR ',') AS categories
            FROM stall WHERE deleted = 0 AND status = 1 AND stock > 0
              AND painter_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND category IS NOT NULL AND category != ''
            GROUP BY painter_id
            </script>
            """)
    java.util.List<com.artiv.dto.ArtistCategoryAggregate> selectGuideCategories(
            @org.apache.ibatis.annotations.Param("ids") java.util.List<Long> ids);

    @org.apache.ibatis.annotations.Select("SELECT * FROM stall WHERE id = #{id} FOR UPDATE")
    Stall selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
