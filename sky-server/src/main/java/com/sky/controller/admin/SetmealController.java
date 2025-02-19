package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:43
 **/
@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Resource
    private SetmealService setmealService;

    @PostMapping
    public Mono<Result<String>> addSetmeal(@RequestBody SetmealDTO setmealDTO) {
        return setmealService.addSetmeal(setmealDTO);
    }

    @GetMapping("/page")
    public Mono<Result<PageResult>> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        return setmealService.page(setmealPageQueryDTO);
    }

    @PostMapping("/status/{status}")
    public Mono<Result<String>> editStatus(@PathVariable("status") Integer status, Long id) {
        return setmealService.editStatus(status, id);
    }

    @GetMapping("/{id}")
    public Mono<Result<SetmealVO>> getSetmealById(@PathVariable("id") Long id) {
        return setmealService.getSetmealById(id);
    }

    @PutMapping
    public Mono<Result<String>> editSetmeal(@RequestBody SetmealDTO setmealDTO) {
        return setmealService.editSetmeal(setmealDTO);
    }

    @DeleteMapping
    public Mono<Result<String>> deleteSetmeal(@RequestParam("ids") List<Long> ids) {
        return setmealService.deleteSetmeal(ids);
    }
}
