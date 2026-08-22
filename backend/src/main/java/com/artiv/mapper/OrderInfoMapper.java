package com.artiv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.artiv.dto.StallSalesCount;
import com.artiv.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    @Select("SELECT * FROM order_info WHERE id = #{id} FOR UPDATE")
    OrderInfo selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            <script>
            SELECT stall_id AS stallId, COUNT(*) AS soldCount
            FROM order_info
            WHERE deleted = 0 AND status = 3 AND stall_id IN
            <foreach collection="stallIds" item="stallId" open="(" separator="," close=")">
                #{stallId}
            </foreach>
            GROUP BY stall_id
            </script>
            """)
    List<StallSalesCount> selectCompletedSalesByStallIds(@Param("stallIds") List<Long> stallIds);
}
