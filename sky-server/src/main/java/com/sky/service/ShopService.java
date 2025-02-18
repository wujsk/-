package com.sky.service;

import com.sky.result.Result;
import reactor.core.publisher.Mono;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 19:12
 **/
public interface ShopService {

    Mono<Result<Integer>> getStatus();

    Mono<Result<String>> setStatus(Integer status);

}
