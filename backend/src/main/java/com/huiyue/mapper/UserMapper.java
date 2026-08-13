package com.huiyue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huiyue.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM user WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
