package com.huiyue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huiyue.entity.Stall;

public interface StallMapper extends BaseMapper<Stall> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM stall WHERE id = #{id} FOR UPDATE")
    Stall selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
