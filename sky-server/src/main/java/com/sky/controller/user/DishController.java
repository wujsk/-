package com.sky.controller.user;

import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:48
 **/
@RestController("userDishController")
@RequestMapping("/user/dish")
public class DishController {

    @Resource
    private DishService dishService;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Mono<Result<List<DishVO>>> list(Long categoryId) {
        return dishService.list(categoryId);
    }
}
