package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealDishRepository;
import com.sky.repository.SetmealRepository;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static cn.dev33.satoken.SaManager.log;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 15:07
 **/
@Service
public class SetmealServiceImpl implements SetmealService {

    @Resource
    private SetmealRepository setmealRepository;

    @Resource
    private SetmealDishRepository setmealDishRepository;

    @Resource
    private DishRepository dishRepository;

    @Resource
    private ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

    private Result<String> setmealResult(SetmealDTO setmealDTO) {
        if (Objects.isNull(setmealDTO)) {
            return Result.error("添加套餐失败");
        }
        if (StrUtil.isBlank(setmealDTO.getName())) {
            return Result.error("套餐名称不能为空");
        }
        BigDecimal price = setmealDTO.getPrice();
        if (Objects.isNull(price) || price.scale() > 2) {
            return Result.error("套餐价格输入错误，请重新输入");
        }
        if (setmealDTO.getDescription().length() > 200) {
            return Result.error("套餐描述超过200字");
        }
        return null;
    }

    @Override
    @Transactional
    public Mono<Result<String>> addSetmeal(SetmealDTO setmealDTO) {
        Result<String> r = setmealResult(setmealDTO);
        if (!Objects.isNull(r)) {
            return Mono.just(r);
        }
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmeal.setCreateTime(LocalDateTime.now());
        setmeal.setUpdateTime(LocalDateTime.now());
        setmeal.setCreateUser(StpUtil.getLoginIdAsLong());
        setmeal.setUpdateUser(StpUtil.getLoginIdAsLong());
        return setmealRepository.save(setmeal)
                .flatMap(savedSetmeal  -> {
                    List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
                    if (CollectionUtil.isNotEmpty(setmealDishes)) {
                        setmealDishes.forEach(dish -> dish.setSetmealId(savedSetmeal.getId()));
                        return setmealDishRepository.saveAll(setmealDishes)
                                .then(reactiveRedisTemplate.delete("setmeal:" + setmealDTO.getCategoryId()))
                                .thenReturn(Result.success("添加套餐成功"));
                    }
                    return reactiveRedisTemplate.delete("setmeal:" + setmealDTO.getCategoryId())
                            .thenReturn(Result.success("添加套餐成功"));
                }).onErrorReturn(Result.error("添加套餐失败"));
    }

    @Override
    public Mono<Result<List<SetmealVO>>> list(Long categoryId) {
        return reactiveRedisTemplate.opsForValue().get("setmeal:" + categoryId)
                .cast(String.class)
                .flatMap(setmealStr -> {
                    List<SetmealVO> setmealVOList = JSON.parseArray(setmealStr, SetmealVO.class);
                    return Mono.just(Result.success(setmealVOList));
                })
                .switchIfEmpty(Mono.just("")
                        .flatMap(s -> {
                            Setmeal setmeal = Setmeal.builder()
                                    .categoryId(categoryId)
                                    .status(StatusConstant.ENABLE)
                                    .build();
                            return setmealRepository.querySetmealListByCategoryIdAndStatus(setmeal)
                                    .collectList()
                                    .flatMap(setmealVOList -> {
                                        if (CollectionUtil.isEmpty(setmealVOList)) {
                                            return Mono.just(Result.success(Collections.emptyList()));
                                        }
                                        return reactiveRedisTemplate.opsForValue()
                                                .set("setmeal:" + categoryId, JSON.toJSONString(setmealVOList))
                                                .thenReturn(Result.success(setmealVOList));
                                    });
                        })
                );
    }


    @Override
    public Mono<Result<PageResult>> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        int page = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        String name = setmealPageQueryDTO.getName();
        Long categoryId = setmealPageQueryDTO.getCategoryId();
        Integer status = setmealPageQueryDTO.getStatus();
        return setmealRepository
                .querySetmealList(name, categoryId, status, pageable)
                .collectList()
                .zipWith(setmealRepository.countSetmeals(name, categoryId, status))
                .flatMap(tuple -> {
                    List<SetmealVO> setmealVOList = tuple.getT1();
                    long total = tuple.getT2();
                    return Mono.just(Result.success(new PageResult(total, setmealVOList)));
                });
    }

    @Override
    public Mono<Result<String>> editStatus(Integer status, Long id) {
        if (Objects.isNull(id)) {
            return Mono.just(Result.error("请选择你要更改的套餐"));
        }
        if (status == 1) {
            return setmealDishRepository.countStatusBySetmealId(id, StatusConstant.DISABLE)
                    .flatMap(count -> {
                        if (count > 0) {
                            return Mono.error(new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED));
                        }
                        return setmealRepository.queryCategoryIdById(id)
                                .flatMap(categoryId -> {
                                    Setmeal setmeal = Setmeal.builder()
                                            .status(status)
                                            .id(id)
                                            .build();
                                    return setmealRepository.updateSetmeal(setmeal)
                                            .flatMap(updateResult -> {
                                                if (updateResult < 1) {
                                                    return Mono.just(Result.error("更改失败"));
                                                }
                                                return reactiveRedisTemplate.delete("setmeal:" + categoryId)
                                                        .thenReturn(Result.success("更改成功"));
                                            });
                                });
                    });
        }
        return setmealRepository.queryCategoryIdById(id)
                .flatMap(categoryId -> {
                    Setmeal setmeal = Setmeal.builder()
                            .status(status)
                            .id(id)
                            .build();
                    return setmealRepository.updateSetmeal(setmeal)
                            .flatMap(updateResult -> {
                                if (updateResult < 1) {
                                    return Mono.just(Result.error("更改失败"));
                                }
                                return reactiveRedisTemplate.delete("setmeal:" + categoryId)
                                        .thenReturn(Result.success("更改成功"));
                            });
                });
    }

    @Override
    public Mono<Result<SetmealVO>> getSetmealById(Long id) {
        if (Objects.isNull(id)) {
            return Mono.just(Result.error("请选择你要查询的套餐"));
        }
        return setmealRepository.querySetmealById(id)
                .flatMap(setmealVO -> {
                    if (Objects.isNull(setmealVO)) {
                        return Mono.just(Result.error("暂无该套餐"));
                    }
                    return setmealDishRepository.querySetmealBySetmealId(id)
                            .collectList()
                            .map(setmealDishes -> {
                                setmealVO.setSetmealDishes(setmealDishes);
                                return Result.success(setmealVO);
                            });
                });
    }

    @Override
    @Transactional
    public Mono<Result<String>> editSetmeal(SetmealDTO setmealDTO) {
        Result<String> r = setmealResult(setmealDTO);
        if (!Objects.isNull(r)) {
            return Mono.just(r);
        }
        return setmealRepository.queryCategoryIdById(setmealDTO.getId())
                .flatMap(oldCategoryId -> {
                    Long newCategoryId = setmealDTO.getCategoryId();
                    Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
                    setmeal.setUpdateTime(LocalDateTime.now());
                    setmeal.setUpdateUser(StpUtil.getLoginIdAsLong());
                    return setmealRepository.updateSetmeal(setmeal)
                            .flatMap(updateResult -> {
                                if (updateResult < 1) {
                                    return Mono.just(Result.error("更新套餐失败"));
                                }
                                Long setmealId = setmealDTO.getId();
                                return setmealDishRepository.deleteBySetmealIds(Collections.singletonList(setmealId))
                                        .thenMany(Flux.fromIterable(setmealDTO.getSetmealDishes()))
                                        .doOnNext(dish -> dish.setSetmealId(setmealId))
                                        .collectList()
                                        .flatMapMany(setmealDishes -> setmealDishRepository.saveAll(setmealDishes))
                                        .then(reactiveRedisTemplate.delete("setmeal:" + oldCategoryId))
                                        .then(Objects.equals(oldCategoryId, newCategoryId) ? Mono.empty() : reactiveRedisTemplate.delete("setmeal:" + newCategoryId))
                                        .thenReturn(Result.success("更新成功"));
                            });
                });
    }

    @Override
    @Transactional
    public Mono<Result<String>> deleteSetmeal(List<Long> ids) {
        return setmealRepository.countStatus(StatusConstant.ENABLE, ids)
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.error(new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE));
                    }
                    return setmealRepository.deleteSetmealByIds(ids)
                            .flatMap(deleteResult -> {
                                if (deleteResult < 1) {
                                    return Mono.just(Result.error("删除失败"));
                                }
                                return setmealDishRepository.deleteBySetmealIds(ids)
                                        .thenReturn(Result.success("删除成功"))
                                        .onErrorResume(e -> {
                                            log.error("删除套餐关联的菜品失败", e);
                                            return Mono.just(Result.error("删除失败"));
                                        });
                            });
                });
    }

    @Override
    public Mono<Result<List<DishItemVO>>> getDishItemById(Long id) {
        if (id == null) {
            return Mono.just(Result.error("查询失败"));
        }
        return dishRepository.queryDishItemVoList(id)
                .collectList()
                .flatMap(dishItemVOList -> {
                    if (CollectionUtil.isEmpty(dishItemVOList)) {
                        dishItemVOList = Collections.emptyList();
                    }
                    return Mono.just(Result.success(dishItemVOList));
                });
    }

}
