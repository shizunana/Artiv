package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Invitation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface InvitationMapper extends BaseMapper<Invitation> {
    @Update("""
            UPDATE invitation i
            JOIN project p ON p.id = i.project_id
            SET i.status = 3, i.responded_at = #{endedAt}, i.updated_at = #{endedAt}
            WHERE i.status = 0 AND i.deleted = 0
              AND ((p.employer_id = #{firstUserId} AND i.painter_id = #{secondUserId})
                OR (p.employer_id = #{secondUserId} AND i.painter_id = #{firstUserId}))
            """)
    int invalidateBetweenUsers(@Param("firstUserId") Long firstUserId,
                               @Param("secondUserId") Long secondUserId,
                               @Param("endedAt") LocalDateTime endedAt);
}
