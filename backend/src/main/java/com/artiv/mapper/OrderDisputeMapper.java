package com.artiv.mapper;

import com.artiv.entity.OrderDispute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderDisputeMapper extends BaseMapper<OrderDispute> {
    @Select("SELECT * FROM order_dispute WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    OrderDispute selectByIdForUpdate(@Param("id") Long id);
}
