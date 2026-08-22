package com.artiv.mapper;

import com.artiv.entity.ReviewAppeal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface ReviewAppealMapper extends BaseMapper<ReviewAppeal> {

    @Select("SELECT * FROM review_appeal WHERE id = #{id} FOR UPDATE")
    ReviewAppeal selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            <script>
            SELECT ra.*
            FROM review_appeal ra
            JOIN `user` u ON u.id = ra.applicant_id AND u.deleted = 0
            WHERE 1 = 1
            <if test="status != null">AND ra.status = #{status}</if>
            <if test="keyword != null and keyword != ''">
                AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="startAt != null">AND ra.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ra.created_at &lt; #{endAt}</if>
            ORDER BY ra.status ASC, ra.created_at DESC, ra.id DESC
            </script>
            """)
    Page<ReviewAppeal> selectAdminPage(Page<ReviewAppeal> page,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);
}
