package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Application;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ApplicationMapper extends BaseMapper<Application> {
    @Select("SELECT * FROM application WHERE id = #{id} FOR UPDATE")
    Application selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE application a
            JOIN project p ON p.id = a.project_id
            SET a.active = 0, a.ended_reason = 'BLOCKED',
                a.ended_at = #{endedAt}, a.updated_at = #{endedAt}
            WHERE a.active = 1 AND a.deleted = 0
              AND ((p.employer_id = #{firstUserId} AND a.painter_id = #{secondUserId})
                OR (p.employer_id = #{secondUserId} AND a.painter_id = #{firstUserId}))
            """)
    int endBetweenUsers(@Param("firstUserId") Long firstUserId,
                        @Param("secondUserId") Long secondUserId,
                        @Param("endedAt") LocalDateTime endedAt);
}
