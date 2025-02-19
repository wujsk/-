package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SetmealService {

    /**
     * 添加套餐
     * @param setmealDTO
     * @return
     */
    Mono<Result<String>> addSetmeal(SetmealDTO setmealDTO);

    Mono<Result<List<SetmealVO>>> list(Long categoryId);

    /**
     * 查询套餐列表
     * @param setmealPageQueryDTO
     * @return
     */
    Mono<Result<PageResult>> page(SetmealPageQueryDTO setmealPageQueryDTO);

    Mono<Result<String>> editStatus(Integer status, Long id);

    Mono<Result<SetmealVO>> getSetmealById(Long id);

    Mono<Result<String>> editSetmeal(SetmealDTO setmealDTO);

    Mono<Result<String>> deleteSetmeal(List<Long> ids);

    Mono<Result<List<DishItemVO>>> getDishItemById(Long id);
}
