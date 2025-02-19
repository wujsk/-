package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * @author: cyy
 * @create: 2025-02-18 13:43
 **/
@RestController("adminOrderController")
@RequestMapping("/admin/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        return orderService.pageList(ordersPageQueryDTO);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        return orderService.statistics();
    }

    @GetMapping("/details/{id}")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return orderService.getOrderDetail(id);
    }

    @PutMapping("/confirm")
    public Result<String> confirm(@RequestBody Orders orders) {
        return orderService.confirm(orders);
    }

    @PutMapping("/rejection")
    public Result<String> rejection(@RequestBody Orders orders) {
        return orderService.rejection(orders);
    }

    @PutMapping("/delivery/{id}")
    public Result<String> delivery(@PathVariable("id") Long id) {
        return orderService.delivery(id);
    }

    @PutMapping("/cancel")
    public Result<String> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        return orderService.adminCancel(ordersCancelDTO);
    }

    @PutMapping("/complete/{id}")
    public Result<String> complete(@PathVariable("id") Long id) {
        return orderService.complete(id);
    }

}
