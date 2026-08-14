package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Stall;

public interface StallMapper extends BaseMapper<Stall> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM stall WHERE id = #{id} FOR UPDATE")
    Stall selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
