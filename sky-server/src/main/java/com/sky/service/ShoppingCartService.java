package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;

import java.util.List;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-18 10:55
 **/
public interface ShoppingCartService {

    Result<String> add(ShoppingCartDTO shoppingCartDTO);

    Result<List<ShoppingCart>> list();

    Result<String> sub(ShoppingCartDTO shoppingCartDTO);

    Result<String> clean();

}
