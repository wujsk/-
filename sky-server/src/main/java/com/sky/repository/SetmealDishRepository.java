package com.sky.repository;

import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface SetmealDishRepository extends R2dbcRepository<SetmealDish, Long> {

    @Query("SELECT id FROM t_setmeal_dish WHERE dish_id IN (:dishIds)")
    Flux<Long> querySetmealByDishIds(List<Long> dishIds);

    @Query("select * from setmeal_dish where setmeal_id = :setmealId")
    Flux<SetmealDish> querySetmealBySetmealId(Long setmealId);

    @Modifying
    @Query("DELETE FROM t_setmeal_dish WHERE setmeal_id IN (:setmealId)")
    Mono<Long> deleteBySetmealIds(List<Long> setmealIds);

    @Query("SELECT COUNT(1) FROM t_setmeal_dish WHERE setmeal_id = :setmealId AND status = :status")
    Mono<Long> countStatusBySetmealId(Long setmealId, Integer status);
}
