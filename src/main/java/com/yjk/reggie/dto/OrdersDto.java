package com.yjk.reggie.dto;

import com.yjk.reggie.entity.OrderDetail;
import com.yjk.reggie.entity.Orders;
import lombok.Data;
import java.util.List;

@Data
public class OrdersDto extends Orders {

    private List<OrderDetail> orderDetails;
	
}
