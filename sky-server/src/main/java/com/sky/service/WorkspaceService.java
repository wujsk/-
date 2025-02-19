package com.sky.service;

import com.sky.result.Result;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    Mono<Result<BusinessDataVO>> getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return
     */
    Mono<Result<OrderOverViewVO>> getOrderOverView();

    /**
     * 查询菜品总览
     * @return
     */
    Mono<Result<DishOverViewVO>> getDishOverView();

    /**
     * 查询套餐总览
     * @return
     */
    Mono<Result<SetmealOverViewVO>> getSetmealOverView();

}
