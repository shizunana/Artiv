package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    @Select("SELECT * FROM order_info WHERE id = #{id} FOR UPDATE")
    OrderInfo selectByIdForUpdate(@Param("id") Long id);
}
