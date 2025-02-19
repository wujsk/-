package com.sky.controller.user;

import jakarta.annotation.Resource;
import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:52
 **/
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
public class SetmealController {

    @Resource
    private SetmealService setmealService;

    @Resource
    private DishService dishService;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Mono<Result<List<SetmealVO>>> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        return setmealService.list(categoryId);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    public Mono<Result<List<DishItemVO>>> dishList(@PathVariable("id") Long id) {
        return setmealService.getDishItemById(id);
    }
}
