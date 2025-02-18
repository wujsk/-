package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-16 15:47
 **/
public interface DishService {

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    Mono<Result<String>> addDish(DishDTO dishDTO);

    /**
     * 菜品分页
     * @param dishPageQueryDTO
     * @return
     */
    Mono<Result<PageResult>> page(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id删除菜品
     * @param ids
     * @return
     */
    Mono<Result<String>> deleteDish(List<Long> ids);

    /**
     * 根据id查找菜品
     * @param id
     * @return
     */
    Mono<Result<DishDTO>> getDishById(Long id);

    /**
     * 修改菜品状态
     * @param status
     * @param id
     * @return
     */
    Mono<Result<String>> editDishStatus(Integer status, Long id);

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    Mono<Result<String>> editDish(DishDTO dishDTO);

    /**
     * 根据分类查找菜品
     * @param categoryId
     * @return
     */
    Mono<Result<List<DishVO>>> list(Long categoryId);
}
