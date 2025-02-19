package com.sky.service;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OrderService {

    /**
     * 提交订单
     * @param submitDTO
     * @return
     */
    Mono<Result<OrderSubmitVO>> submit(OrdersSubmitDTO submitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    Mono<Result<OrderPaymentVO>> payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    Mono<Void> paySuccess(String outTradeNo);

    /**
     * 查看历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Mono<Result<PageResult>> pageList(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    Mono<Result<OrderVO>> getOrderDetail(Long id);

    /**
     * 用户取消订单
     * @param id
     * @return
     */
    Mono<Result<String>> cancel(Orders orders);

    /**
     * 商家取消订单
     * @param cancelDTO
     * @return
     */
    Mono<Result<String>> adminCancel(OrdersCancelDTO cancelDTO);

    /**
     * 再来一单
     * @param id
     * @return
     */
    Mono<Result<String>> repetition(Long id);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    Mono<Result<OrderStatisticsVO>> statistics();

    /**
     * 接单
     * @param orders
     * @return
     */
    Mono<Result<String>> confirm(Orders orders);

    /**
     * 拒单
     * @param orders
     * @return
     */
    Mono<Result<String>> rejection(Orders orders);

    /**
     * 派单
     * @param id
     * @return
     */
    Mono<Result<String>> delivery(Long id);

    /**
     * 完成派单
     * @param id
     * @return
     */
    Mono<Result<String>> complete(Long id);

    Mono<Void> reminder(Long id);
}
