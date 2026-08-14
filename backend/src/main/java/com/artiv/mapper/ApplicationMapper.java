package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.Application;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ApplicationMapper extends BaseMapper<Application> {
    @Select("SELECT * FROM application WHERE id = #{id} FOR UPDATE")
    Application selectByIdForUpdate(@Param("id") Long id);
}
