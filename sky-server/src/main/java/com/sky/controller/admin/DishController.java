package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:43
 **/
@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Resource
    private DishService dishService;

    @PostMapping
    public Mono<Result<String>> addDish(@RequestBody DishDTO dishDTO) {
        return dishService.addDish(dishDTO);
    }

    @GetMapping("/page")
    public Mono<Result<PageResult>> page(DishPageQueryDTO dishPageQueryDTO) {
        return dishService.page(dishPageQueryDTO);
    }

    @DeleteMapping
    public Mono<Result<String>> deleteDish(@RequestParam("ids") List<Long> ids) {
        return dishService.deleteDish(ids);
    }

    @GetMapping("{id}")
    public Mono<Result<DishDTO>> getDishById(@PathVariable("id") Long id) {
        return dishService.getDishById(id);
    }

    @PostMapping("/status/{status}")
    public Mono<Result<String>> editDishStatus(@PathVariable("status") Integer status,  Long id) {
        return dishService.editDishStatus(status, id);
    }

    @PutMapping
    public Mono<Result<String>> editDish(@RequestBody DishDTO dishDTO) {
        return dishService.editDish(dishDTO);
    }

    @GetMapping("/list")
    public Mono<Result<List<DishVO>>> list(Long categoryId) {
        return dishService.list(categoryId);
    }
}
