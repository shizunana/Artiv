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
}
