package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

/**
 * @author: cyy
 * @create: 2025-02-18 13:44
 **/
@RestController("adminShopController")
@RequestMapping("/admin/shop")
public class ShopController {

    @Resource
    private ShopService shopService;

    @GetMapping("/status")
    public Mono<Result<Integer>> getStatus() {
        return shopService.getStatus();
    }

    @PutMapping("/{status}")
    public Mono<Result<String>> setStatus(@PathVariable("status") Integer status) {
        return shopService.setStatus(status);
    }

}
