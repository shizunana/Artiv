package com.artiv.mapper;

import com.artiv.entity.ArtistCertification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArtistCertificationMapper extends BaseMapper<ArtistCertification> {
    @Select("SELECT * FROM artist_certification WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ArtistCertification selectByIdForUpdate(@Param("id") Long id);
}
