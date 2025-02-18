package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:53
 **/
public class ShoppingCartController {

    @Resource
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        return shoppingCartService.add(shoppingCartDTO);
    }

    /**
     * 购物车列表
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list() {
        return shoppingCartService.list();
    }

    /**
     * 减少某个商品的数量
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        return shoppingCartService.sub(shoppingCartDTO);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public Result<String> clean() {
        return shoppingCartService.clean();
    }
}
