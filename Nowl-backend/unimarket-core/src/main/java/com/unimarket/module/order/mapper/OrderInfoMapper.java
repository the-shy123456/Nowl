package com.unimarket.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * OrderInfo Mapper接口
 */
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

/**
 * OrderInfo Mapper接口
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 统计完成订单的总金额
     * @param status 订单状态
     * @param startTime 开始时间（可选）
     * @return 总金额
     */
    @Select("<script>" +
            "SELECT COALESCE(SUM(total_amount), 0) FROM order_info " +
            "WHERE order_status = #{status} " +
            "<if test='startTime != null'>AND create_time >= #{startTime}</if>" +
            "</script>")
    BigDecimal selectTotalAmount(@Param("status") Integer status, @Param("startTime") java.time.LocalDateTime startTime);
}
