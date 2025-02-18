package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.sky.constant.MessageConstant;
import com.sky.constant.SqlConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.repository.DishFlavorRepository;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealDishRepository;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-16 15:47
 **/
@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Resource
    private DishRepository dishRepository;

    @Resource
    private DishFlavorRepository dishFlavorRepository;

    @Resource
    private SetmealDishRepository setmealDishRepository;

    @Resource
    private ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

    @Resource
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    private Result<String> dishResult(DishDTO dishDTO) {
        if (Objects.isNull(dishDTO)) {
            return Result.error("添加菜品失败");
        }
        if (StrUtil.isBlank(dishDTO.getName())) {
            return Result.error("菜品名称不能为空");
        }
        BigDecimal price = dishDTO.getPrice();
        if (Objects.isNull(price) || price.scale() > 2) {
            return Result.error("菜品价格输入错误，请重新输入");
        }
        if (dishDTO.getDescription().length() > 200) {
            return Result.error("菜品描述超过200字");
        }
        return null;
    }

    @Override
    @Transactional
    public Mono<Result<String>> addDish(DishDTO dishDTO) {
        Result<String> r = dishResult(dishDTO);
        if (!Objects.isNull(r)) {
            return Mono.just(r);
        }
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dish.setStatus(StatusConstant.ENABLE);
        return dishRepository.save(dish)
                .flatMap(dishObj -> {
                    List<DishFlavor> flavors = dishDTO.getFlavors();
                    if (CollectionUtil.isNotEmpty(flavors)) {
                        return dishFlavorRepository.saveDishFlavors(dish.getId(), flavors)
                                .flatMap(count -> {
                                    if (count < 1) {
                                        return Mono.just(Result.<String>error("添加菜品失败"));
                                    }
                                    return reactiveRedisTemplate.delete("dish:" + dish.getCategoryId())
                                            .then(Mono.just(Result.success("添加菜品成功")));
                                });
                    }
                    return Mono.just(Result.<String>error("请选择口味"));
                })
                .onErrorResume(e -> {
                    log.info("出现异常：{}", e.getMessage());
                    return Mono.just(Result.error("添加菜品失败"));
                });
    }

    @Override
    public Mono<Result<PageResult>> page(DishPageQueryDTO dishPageQueryDTO) {
        int page = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        Pageable pageable = PageRequest.of(page, pageSize);
        Dish dish = BeanUtil.copyProperties(dishPageQueryDTO, Dish.class);
        return dishRepository.findDishVoByQuery(dish, pageable)
                .collectList()
                .flatMap(vo -> dishRepository.countByQuery(dish)
                        .zipWith(Mono.just(vo))
                        .map(tuple -> Result.success(new PageResult(tuple.getT1(), tuple.getT2()))))
                .onErrorResume(e -> {
                    log.info("出现异常：{}", e.getMessage());
                    return Mono.error(new BaseException("查询失败"));
                });
    }

    @Override
    @Transactional
    public Mono<Result<String>> deleteDish(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            log.info("未选择要删除的菜品，返回错误信息");
            return Mono.just(Result.error("请选择你要删除的菜品"));
        }

        return checkDishOnSale(ids)
                .flatMap(result -> checkDishRelatedToSetmeal(ids))
                .flatMap(result -> deleteDishesAndFlavors(ids))
                .flatMap(result -> clearRedisCache(ids))
                .flatMap(result -> Mono.just(Result.success("删除成功")))
                .doOnError(e -> log.error("删除菜品时出现异常", e));
    }

    private Mono<Result<String>> checkDishOnSale(List<Long> ids) {
        return dishRepository.queryCountByStatus(StatusConstant.ENABLE, ids)
                .flatMap(count -> {
                    if (count > 0) {
                        log.info("菜品正在售卖中，无法删除，返回错误信息");
                        return Mono.just(Result.error(MessageConstant.DISH_ON_SALE));
                    }
                    return Mono.just(Result.success(""));
                });
    }

    private Mono<Result<String>> checkDishRelatedToSetmeal(List<Long> ids) {
        return setmealDishRepository.querySetmealByDishIds(ids)
                .collectList()
                .flatMap(list -> {
                    if (!isEmpty(list)) {
                        log.info("菜品被套餐关联，无法删除，返回错误信息");
                        return Mono.just(Result.error(MessageConstant.DISH_BE_RELATED_BY_SETMEAL));
                    }
                    return Mono.just(Result.success(""));
                });
    }

    private Mono<Result<String>> deleteDishesAndFlavors(List<Long> ids) {
        return dishRepository.deleteByIds(ids)
                .flatMap(count -> {
                    if (count < 1) {
                        log.info("删除菜品失败，返回错误信息");
                        return Mono.just(Result.error("删除失败"));
                    }
                    return dishFlavorRepository.deleteDishFlavorsByDishId(ids)
                            .flatMap(count2 -> {
                                if (count2 < 1) {
                                    log.info("删除菜品口味失败，返回错误信息");
                                    return Mono.just(Result.error("删除失败"));
                                }
                                return Mono.just(Result.success(""));
                            });
                });
    }

    private Mono<Result<String>> clearRedisCache(List<Long> ids) {
        return dishRepository.queryCategoryIdByIds(ids)
                .map(id -> "dish:" + id)
                .collectList()
                .flatMap(keys -> reactiveRedisTemplate.delete(keys))
                .map(deleted -> {
                    if (deleted > 0) {
                        log.info("删除菜品成功，清除 Redis 缓存");
                        return Result.success("删除成功");
                    }
                    log.info("删除菜品成功，但清除 Redis 缓存失败");
                    return Result.success("删除成功，但清除缓存时出现问题");
                });
    }

    @Override
    public Mono<Result<DishDTO>> getDishById(Long id) {
        return Mono.just(id)
                .switchIfEmpty(Mono.error(new BaseException("请选择你要查询的菜品")))
                .flatMap(idx -> dishRepository.queryDishById(idx))
                .switchIfEmpty(Mono.error(new BaseException("暂无该菜品")))
                .flatMap(dish -> dishFlavorRepository.queryDishFlavorByDishId(dish.getId())
                        .collectList()
                        .flatMap(flavors -> {
                            dish.setFlavors(flavors);
                            return Mono.just(Result.success(dish));
                        }));
    }

    @Override
    public Mono<Result<String>> editDishStatus(Integer status, Long id) {
        return Mono.just(id)
                .switchIfEmpty(Mono.error(new BaseException("请选择你要更改的菜品")))
                .flatMap(idx -> Mono.just(Dish.builder()
                        .status(status)
                        .id(id)
                        .updateTime(LocalDateTime.now())
                        .updateUser(StpUtil.getLoginIdAsLong())
                        .build()))
                .flatMap(dish -> dishRepository.updateDish(dish)
                        .flatMap(count -> {
                            if (count < 1) {
                                return Mono.just(Result.error("更改失败"));
                            }
                            return dishRepository.queryDishById(id)
                                    .flatMap(dto -> reactiveRedisTemplate.delete("dish:" + dto.getCategoryId())
                                            .then(Mono.just(Result.success("更改成功"))));
                        }));
    }

    @Override
    @Transactional
    public Mono<Result<String>> editDish(DishDTO dishDTO) {
        Result<String> r = dishResult(dishDTO);
        if (!Objects.isNull(r)) {
            return Mono.just(r);
        }
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        return Mono.just(dish)
                .flatMap(dish1 -> dishRepository.updateDish(dish1))
                .flatMap(count -> {
                    if (count < 1) {
                        return Mono.just(Result.error("更新菜品失败"));
                    }
                    return Mono.just(dish);
                })
                .flatMap(dish2 -> dishRepository.queryDishById(dishDTO.getId()))
                .flatMap(dto -> {
                    List<DishFlavor> flavors = dto.getFlavors();
                    if (CollectionUtil.isNotEmpty(flavors)) {
                        return Flux.fromIterable(flavors)
                                .buffer(SqlConstant.MAX_BATCH)
                                .flatMap(batch -> {
                                    Flux<Long> batchUpdateFlux = Flux.fromIterable(batch)
                                            .flatMap(flavor -> {
                                                Update update = Update.update("name", flavor.getName())
                                                        .set("price", flavor.getValue());
                                                Query query = Query.query(Criteria.where("id").is(flavor.getId()));
                                                return r2dbcEntityTemplate.update(DishFlavor.class)
                                                        .matching(query)
                                                        .apply(update);
                                            });
                                    return batchUpdateFlux.reduce(0L, Long::sum);
                                })
                                .reduce(0L, Long::sum)
                                .flatMap(count -> {
                                    if (count < 1) {
                                        return Mono.just(Result.<String>error("更新菜品失败"));
                                    }
                                    Long oldCategoryId = dto.getCategoryId();
                                    Long newCategoryId = dishDTO.getCategoryId();
                                    if (!Objects.equals(oldCategoryId, newCategoryId)) {
                                        List<String> categoryIds = Arrays.asList("dish:" + oldCategoryId, "dish:" + newCategoryId);
                                        return reactiveRedisTemplate.delete(categoryIds)
                                                .then(Mono.just(Result.success("更新菜品成功")));
                                    } else {
                                        return reactiveRedisTemplate.delete("dish:" + newCategoryId)
                                                .then(Mono.just(Result.success("更新菜品成功")));
                                    }
                                });
                    }
                    return Mono.just(Result.<String>error("请选择口味"));
                });
    }

    @Override
    public Mono<Result<List<DishVO>>> list(Long categoryId) {
        return Mono.just(categoryId)
                .switchIfEmpty(Mono.error(new BaseException("请选择你要查询的菜品")))
                .flatMap(id -> {
                    return reactiveRedisTemplate.opsForValue().get("dish:" + categoryId);
                })
                .flatMap(str -> {
                    if (StrUtil.isNotBlank((String) str)) {
                        List<DishVO> list = JSON.parseArray((String) str, DishVO.class);
                        if (CollectionUtil.isEmpty(list)) {
                            return Mono.just(Result.success(Collections.emptyList()));
                        }
                        final List<DishVO> finalList = list;
                        return reactiveRedisTemplate.expire("dish:" + categoryId, Duration.ofMinutes(5))
                                .flatMap(result -> Mono.just(Result.success(finalList)));
                    }
                    Dish dish = new Dish();
                    dish.setCategoryId(categoryId);
                    dish.setStatus(StatusConstant.ENABLE);
                    return dishRepository.findDishVo(dish)
                            .collectList()
                            .flatMap(list -> {
                                if (CollectionUtil.isEmpty(list)) {
                                    list = Collections.emptyList();
                                }
                                final List<DishVO> finalList = list;
                                return reactiveRedisTemplate.opsForValue()
                                        .set("dish:" + categoryId, JSON.toJSONString(list), Duration.ofMinutes(5))
                                        .flatMap(setResult -> Mono.just(Result.success(finalList)));
                            });
                });
    }
}
