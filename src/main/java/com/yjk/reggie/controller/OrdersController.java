package com.yjk.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yjk.reggie.common.BaseContext;
import com.yjk.reggie.common.R;
import com.yjk.reggie.dto.OrdersDto;
import com.yjk.reggie.entity.OrderDetail;
import com.yjk.reggie.entity.Orders;
import com.yjk.reggie.entity.ShoppingCart;
import com.yjk.reggie.service.OrderDetailService;
import com.yjk.reggie.service.OrdersService;
import com.yjk.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据:{}", orders);

        ordersService.submit(orders);

        return R.success("下单成功");
    }

    /**
     * 用户分页查询历史订单
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> orders(Integer page, Integer pageSize){

        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getCheckoutTime);
        ordersService.page(pageInfo, queryWrapper);

        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> ordersDtoList = records.stream().map(item -> {

            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);

            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);

            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;

        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(ordersDtoList);

        return R.success(ordersDtoPage);
    }

    /**
     * 后台分页查询订单
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> order(Integer page, Integer pageSize, Long number, String beginTime, String endTime){

        Page<Orders> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(number != null, Orders::getNumber, number);
        queryWrapper.ge(beginTime != null, Orders::getOrderTime, beginTime);
        queryWrapper.le(endTime != null, Orders::getOrderTime, endTime);
        queryWrapper.orderByDesc(Orders::getCheckoutTime);
        ordersService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 订单状态转换
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> finish(@RequestBody Orders orders){

        Long id = orders.getId();

        LambdaUpdateWrapper<Orders> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(Orders::getId, id).set(Orders::getStatus, orders.getStatus());
        ordersService.update(queryWrapper);

        return R.success("已派送");
    }

    /**
     * 再来一单
     * @param orders
     * @return
     */
    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders){

        //获取订单明细
        LambdaQueryWrapper<OrderDetail> orderDetailQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailQueryWrapper.eq(OrderDetail::getOrderId, orders.getId());

        List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailQueryWrapper);

        //清空用户的购物车
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        //将订单明细中的物品加入到购物车中
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(item -> {

            ShoppingCart shoppingCart = new ShoppingCart();

            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setImage(item.getImage());

            Long dishId = item.getDishId();
            Long setmealId = item.getSetmealId();
            if(dishId != null){
                shoppingCart.setDishId(dishId);
            }else {
                shoppingCart.setSetmealId(setmealId);
            }
            shoppingCart.setDishFlavor(item.getDishFlavor());
            shoppingCart.setName(item.getName());
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;

        }).collect(Collectors.toList());

        shoppingCartService.saveBatch(shoppingCartList);

        return R.success("再来一单成功");
    }
}
