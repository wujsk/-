package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

/**
 * @author: cyy
 * @create: 2025-02-18 13:53
 **/
@RestController("userShopController")
@RequestMapping("/user/shop")
public class ShopController {

    @Resource
    private ShopService shopService;

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    public Mono<Result<Integer>> getStatus() {
        return shopService.getStatus();
    }

    /**
     * 设置营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Mono<Result<String>> setStatus(@PathVariable("status") Integer status) {
        return shopService.setStatus(status);
    }
}
