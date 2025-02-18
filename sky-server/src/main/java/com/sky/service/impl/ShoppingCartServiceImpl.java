package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.sky.dto.DishDTO;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.entity.ShoppingCart;
import com.sky.holder.UserHolder;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-18 10:55
 **/
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Resource
    private ShoppingCartMapper shoppingCartMapper;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private SetmealDishMapper setmealDishMapper;

    @Override
    public Result<String> add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart entity = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();
        Long userId = UserHolder.get();
        entity.setUserId(userId);
        ShoppingCart shoppingCartDb = shoppingCartMapper.queryShoppringCart(entity);
        if (Objects.isNull(shoppingCartDb)) {
            ShoppingCart shoppingCart = ShoppingCart.builder()
                    .dishId(dishId)
                    .setmealId(setmealId)
                    .userId(userId)
                    .createTime(LocalDateTime.now())
                    .build();
            if (!Objects.isNull(dishId)) {
                DishDTO dishDTO = dishMapper.queryDishById(dishId);
                shoppingCart.setName(dishDTO.getName())
                        .setImage(dishDTO.getImage())
                        .setNumber(1)
                        .setAmount(dishDTO.getPrice());

            } else {
                Setmeal setmeal = setmealDishMapper.getSetmealById(setmealId);
                shoppingCart.setName(setmeal.getName())
                        .setImage(setmeal.getImage())
                        .setNumber(1)
                        .setAmount(setmeal.getPrice());
            }
            shoppingCartMapper.save(shoppingCart);
        } else {
            Integer number = shoppingCartDb.getNumber();
            shoppingCartDb.setNumber(number + 1);
            shoppingCartMapper.update(shoppingCartDb);
        }
        return Result.success("添加成功");
    }

    @Override
    public Result<List<ShoppingCart>> list() {
        Long userId = UserHolder.get();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(userId);
        if (CollectionUtil.isEmpty(shoppingCartList)) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(shoppingCartList);
    }

    @Override
    public Result<String> sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        shoppingCart.setUserId(UserHolder.get());
        ShoppingCart shoppingCartDb = shoppingCartMapper.queryShoppringCart(shoppingCart);
        if (Objects.isNull(shoppingCartDb)) {
            return Result.error("删除失败");
        }
        Integer number = shoppingCartDb.getNumber();
        if (number == 1) {
            shoppingCartMapper.delete(shoppingCartDb);
        }
        shoppingCart.setNumber(number - 1);
        shoppingCartMapper.update(shoppingCart);
        return Result.success("减少成功");
    }

    @Override
    public Result<String> clean() {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                        .userId(UserHolder.get())
                                .build();
        shoppingCartMapper.delete(shoppingCart);
        return Result.success("清空成功");
    }

}
