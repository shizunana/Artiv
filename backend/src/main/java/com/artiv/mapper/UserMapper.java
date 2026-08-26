package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM user WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Update("""
            UPDATE user
            SET balance = COALESCE(balance, 0) + #{amount}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int incrementBalance(@org.apache.ibatis.annotations.Param("id") Long id,
                         @org.apache.ibatis.annotations.Param("amount") java.math.BigDecimal amount);

    /**
     * 到期解禁必须显式把封禁上下文置空；普通 updateById 会忽略 null 字段。
     * 条件更新同时避免定时任务与请求惰性解禁并发时重复写审计。
     */
    @org.apache.ibatis.annotations.Update("""
            UPDATE user
            SET status = 0,
                ban_reason = NULL,
                banned_by = NULL,
                banned_at = NULL,
                banned_until = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
              AND status = 1
              AND banned_until IS NOT NULL
              AND banned_until <= #{now}
            """)
    int clearExpiredBan(@org.apache.ibatis.annotations.Param("id") Long id,
                        @org.apache.ibatis.annotations.Param("now") java.time.LocalDateTime now);
}
