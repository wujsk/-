package com.sky.repository;

import com.sky.entity.DishFlavor;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 11:04
 **/
@Repository
public interface DishFlavorRepository extends R2dbcRepository<DishFlavor, Long> {

    @Modifying
    @Query("INSERT INTO dish_flavor (dish_id, name, value) VALUES " +
            "#{#flavors.stream().map(flavor -> '(' + #dishId + ', \'' + flavor.name + '\', \'' + flavor.value + '\')').collect(joining(', '))}")
    Mono<Integer> saveDishFlavors(Long dishId, List<DishFlavor> flavors);

    @Modifying
    @Query("DELETE FROM dish_flavor WHERE dish_id IN (:dishIds)")
    Mono<Integer> deleteDishFlavorsByDishId(List<Long> dishIds);

    @Query("select * from dish_flavor where dish_id = :id")
    Flux<DishFlavor> queryDishFlavorByDishId(Long id);

}
