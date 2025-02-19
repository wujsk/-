package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.repository.DishRepository;
import com.sky.repository.OrderRepository;
import com.sky.repository.SetmealRepository;
import com.sky.repository.UserRepository;
import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private DishRepository dishRepository;

    @Resource
    private SetmealRepository setmealRepository;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public Mono<Result<BusinessDataVO>> getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        // 查询总订单数
        Mono<Integer> totalOrderCountMono = orderRepository.countByMap(begin, end, null);
        // 营业额
        Mono<Double> turnoverMono = orderRepository.sumByMap(begin, end, Orders.COMPLETED)
                .defaultIfEmpty(0.0);
        // 有效订单数
        Mono<Integer> validOrderCountMono = orderRepository.countByMap(begin, end, Orders.COMPLETED);
        // 新增用户数
        Mono<Integer> newUsersMono = userRepository.countByMap(begin, end);

        return Mono.zip(totalOrderCountMono, turnoverMono, validOrderCountMono, newUsersMono)
                .flatMap(tuple -> {
                    Integer totalOrderCount = tuple.getT1();
                    Double turnover = tuple.getT2();
                    Integer validOrderCount = tuple.getT3();
                    Integer newUsers = tuple.getT4();

                    Double unitPrice = 0.0;
                    Double orderCompletionRate = 0.0;
                    if (totalOrderCount != 0 && validOrderCount != 0) {
                        // 订单完成率
                        orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
                        // 平均客单价
                        unitPrice = turnover / validOrderCount;
                    }

                    return Mono.just(Result.success(BusinessDataVO.builder()
                            .turnover(turnover)
                            .validOrderCount(validOrderCount)
                            .orderCompletionRate(orderCompletionRate)
                            .unitPrice(unitPrice)
                            .newUsers(newUsers)
                            .build()));
                });
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    @Override
    public Mono<Result<OrderOverViewVO>> getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        // 待接单
        Mono<Integer> waitingOrdersMono = getOrderCount(begin, Orders.TO_BE_CONFIRMED);

        // 待派送
        Mono<Integer> deliveredOrdersMono = getOrderCount(begin, Orders.CONFIRMED);

        // 已完成
        Mono<Integer> completedOrdersMono = getOrderCount(begin, Orders.COMPLETED);

        // 已取消
        Mono<Integer> cancelledOrdersMono = getOrderCount(begin, Orders.CANCELLED);

        // 全部订单
        Mono<Integer> allOrdersMono = getOrderCount(begin, null);

        return Mono.zip(waitingOrdersMono, deliveredOrdersMono, completedOrdersMono, cancelledOrdersMono, allOrdersMono)
                .map(tuple -> Result.success(OrderOverViewVO.builder()
                        .waitingOrders(tuple.getT1())
                        .deliveredOrders(tuple.getT2())
                        .completedOrders(tuple.getT3())
                        .cancelledOrders(tuple.getT4())
                        .allOrders(tuple.getT5())
                        .build()))
                .onErrorResume(e -> Mono.just(Result.error("获取订单管理数据失败")));
    }

    private Mono<Integer> getOrderCount(LocalDateTime begin, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("status", status);
        return orderRepository.countByMap(begin, null, status);
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    @Override
    public Mono<Result<DishOverViewVO>> getDishOverView() {
        // 已售卖菜品数量查询
        Mono<Integer> soldMono = getDishCount(StatusConstant.ENABLE);

        // 已停售菜品数量查询
        Mono<Integer> discontinuedMono = getDishCount(StatusConstant.DISABLE);

        return Mono.zip(soldMono, discontinuedMono)
                .map(tuple -> Result.success(DishOverViewVO.builder()
                        .sold(tuple.getT1())
                        .discontinued(tuple.getT2())
                        .build()))
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.just(Result.error("获取菜品总览数据失败"));
                });
    }

    private Mono<Integer> getDishCount(Integer status) {
        return dishRepository.countByMap(status, null);
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    @Override
    public Mono<Result<SetmealOverViewVO>> getSetmealOverView() {
        // 查询已售卖套餐数量
        Mono<Integer> soldMono = setmealRepository.countByMap(StatusConstant.ENABLE, null);
        // 查询已停售套餐数量
        Mono<Integer> discontinuedMono = setmealRepository.countByMap(StatusConstant.DISABLE, null);

        // 使用 Mono.zip 组合两个 Mono
        return Mono.zip(soldMono, discontinuedMono)
                .map(tuple -> {
                    Integer sold = tuple.getT1();
                    Integer discontinued = tuple.getT2();

                    // 构建 SetmealOverViewVO 对象
                    SetmealOverViewVO setmealOverViewVO = SetmealOverViewVO.builder()
                            .sold(sold)
                            .discontinued(discontinued)
                            .build();

                    // 构建 Result 对象，这里假设 Result 类有一个 success 静态方法用于创建成功响应
                    return Result.success(setmealOverViewVO);
                })
                .onErrorResume(e -> {;
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.just(Result.error("获取套餐总览数据失败"));
                });
    }
}
