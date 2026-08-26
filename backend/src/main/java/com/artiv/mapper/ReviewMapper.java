package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Review;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ReviewMapper extends BaseMapper<Review> {

    @Select("""
            SELECT r.*
            FROM review r
            JOIN order_info o ON o.id = r.order_id
            WHERE r.to_id = #{userId}
              AND r.deleted = 0
              AND o.deleted = 0
              AND ((#{role} = 'painter' AND o.painter_id = r.to_id)
                OR (#{role} = 'employer' AND o.employer_id = r.to_id))
            ORDER BY r.created_at DESC, r.id DESC
            """)
    Page<Review> selectPublicPage(Page<Review> page, @Param("userId") Long userId,
                                  @Param("role") String role);

    @Select("SELECT * FROM review WHERE id = #{id} FOR UPDATE")
    Review selectByIdForUpdate(@Param("id") Long id);

    /**
     * 申诉获批时显式隐藏评价。
     *
     * <p>deleted 是全局逻辑删除字段，普通 updateById 不会把它作为常规字段从 0 更新为 1，
     * 因此这里使用受 deleted = 0 保护的专用更新。</p>
     */
    @Update("""
            UPDATE review
            SET deleted = 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND deleted = 0
            """)
    int hideById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT *
            FROM review
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<Review> selectIncludingDeletedByIds(@Param("ids") List<Long> ids);
}
