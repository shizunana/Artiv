package com.artiv.mapper;

import com.artiv.dto.FavoriteStallItem;
import com.artiv.entity.StallFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StallFavoriteMapper extends BaseMapper<StallFavorite> {

    @Select("""
            <script>
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
              AND s.stock &gt; 0
              AND u.deleted = 0
              AND u.status = 0
              AND u.cert_status = 2
            <if test="category != null and category != ''">
              AND s.category = #{category}
            </if>
            <if test="keyword != null and keyword != ''">
              AND s.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="subject != null and subject != ''">
              AND s.params LIKE CONCAT('%', #{subject}, '%')
            </if>
            <if test="priceMin != null">
              AND s.price &gt;= #{priceMin}
            </if>
            <if test="priceMax != null">
              AND s.price &lt;= #{priceMax}
            </if>
            <if test="deliverTime != null and deliverTime != ''">
              AND s.deliver_time LIKE CONCAT('%', #{deliverTime}, '%')
            </if>
            <choose>
              <when test="sort == 'priceAsc'">ORDER BY s.price ASC, f.created_at DESC, f.id DESC</when>
              <when test="sort == 'priceDesc'">ORDER BY s.price DESC, f.created_at DESC, f.id DESC</when>
              <otherwise>ORDER BY f.created_at DESC, f.id DESC</otherwise>
            </choose>
            </script>
            """)
    Page<FavoriteStallItem> selectMine(Page<FavoriteStallItem> page,
                                       @Param("userId") Long userId,
                                       @Param("category") String category,
                                       @Param("keyword") String keyword,
                                       @Param("sort") String sort,
                                       @Param("subject") String subject,
                                       @Param("priceMin") java.math.BigDecimal priceMin,
                                       @Param("priceMax") java.math.BigDecimal priceMax,
                                       @Param("deliverTime") String deliverTime);
}
