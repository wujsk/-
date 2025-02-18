package com.sky.service.impl;

import com.alibaba.fastjson2.JSON;
import com.sky.result.Result;
import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 19:12
 **/
@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Resource
    private ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

    @Override
    public Mono<Result<Integer>> getStatus() {
        return reactiveRedisTemplate.opsForValue()
                .get("shop:status")
                .switchIfEmpty(Mono.defer(() -> Mono.just("查询店铺状态失败")))
                .flatMap(status -> Mono.just(Result.success((Integer) status)));
    }

    @Override
    public Mono<Result<String>> setStatus(Integer status) {
        return reactiveRedisTemplate.opsForValue()
                .set("shop:status", JSON.toJSONString(status))
                .flatMap(result -> {
                    if (result) {
                        return Mono.just(Result.success("设置成功"));
                    }
                    return Mono.just(Result.success("设置失败"));
                });
    }

}
