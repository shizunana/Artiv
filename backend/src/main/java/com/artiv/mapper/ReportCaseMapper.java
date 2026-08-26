package com.artiv.mapper;

import com.artiv.entity.ReportCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface ReportCaseMapper extends BaseMapper<ReportCase> {
    @Select("SELECT * FROM report_case WHERE id = #{id} FOR UPDATE")
    ReportCase selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            <script>
            SELECT rc.*
            FROM report_case rc
            LEFT JOIN `user` u ON u.id = rc.target_id
            WHERE rc.target_type = 'USER'
            <if test="status != null">AND rc.status = #{status}</if>
            <if test="keyword != null and keyword != ''">
              AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR CAST(rc.target_id AS CHAR) = #{keyword})
            </if>
            <if test="startAt != null">AND rc.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND rc.created_at &lt; #{endAt}</if>
            ORDER BY rc.status ASC, rc.updated_at DESC, rc.id DESC
            </script>
            """)
    Page<ReportCase> selectAdminPage(Page<ReportCase> page,
                                     @Param("status") Integer status,
                                     @Param("keyword") String keyword,
                                     @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt);
}
