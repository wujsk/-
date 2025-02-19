package com.sky.repository;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Repository
public interface DishRepository extends R2dbcRepository<Dish, Long> {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Query("select count(id) from dish where category_id = :categoryId")
    Mono<Long> countByCategoryId(Long categoryId);

    @Query("SELECT d.*, c.name as category_name FROM dish d " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE (:#{#dish.name} IS NULL OR d.name LIKE CONCAT('%', :#{#dish.name}, '%')) " +
            "AND (:#{#dish.categoryId} IS NULL OR d.category_id = :#{#dish.categoryId}) " +
            "AND (:#{#dish.status} IS NULL OR d.status = :#{#dish.status}) " +
            "ORDER BY d.create_time DESC " +
            "LIMIT :#{#pageable.offset}, :#{#pageable.pageSize}")
    Flux<DishVO> findDishVoByQuery(Dish dish, Pageable pageable);

    @Query("SELECT d.*, c.name as category_name FROM dish d " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE (:#{#dish.name} IS NULL OR d.name LIKE CONCAT('%', :#{#dish.name}, '%')) " +
            "AND (:#{#dish.categoryId} IS NULL OR d.category_id = :#{#dish.categoryId}) " +
            "AND (:#{#dish.status} IS NULL OR d.status = :#{#dish.status}) " +
            "ORDER BY d.create_time DESC ")
    Flux<DishVO> findDishVo(Dish dish);

    @Query("SELECT d.*, c.name as category_name FROM dish d " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE (:#{#dish.name} IS NULL OR d.name LIKE CONCAT('%', :#{#dish.name}, '%')) " +
            "AND (:#{#dish.categoryId} IS NULL OR d.category_id = :#{#dish.categoryId}) " +
            "AND (:#{#dish.status} IS NULL OR d.status = :#{#dish.status}) ")
    Mono<Long> countByQuery(Dish dish);

    @Query("SELECT count(id) FROM dish WHERE status = :status AND id IN (:ids)")
    Mono<Integer> queryCountByStatus(Integer status, List<Long> ids);

    @Modifying
    @Query("DELETE FROM dish WHERE id IN (:ids)")
    Mono<Integer> deleteByIds(List<Long> ids);

    @Query("select id, name, category_id, price, image, description, status from dish where id = :id")
    Mono<DishDTO> queryDishById(Long id);

    @Modifying
    @Query("UPDATE dish " +
            "SET name = CASE WHEN :name IS NOT NULL AND :name != '' THEN :name ELSE name END, " +
            "category_id = CASE WHEN :categoryId IS NOT NULL THEN :categoryId ELSE category_id END, " +
            "price = CASE WHEN :price IS NOT NULL THEN :price ELSE price END, " +
            "image = CASE WHEN :image IS NOT NULL AND :image != '' THEN :image ELSE image END, " +
            "description = CASE WHEN :description IS NOT NULL AND :description != '' THEN :description ELSE description END, " +
            "status = CASE WHEN :status IS NOT NULL THEN :status ELSE status END, " +
            "update_time = :updateTime, " +
            "update_user = :updateUser " +
            "WHERE id = :id")
    Mono<Integer> updateDish(Dish dish);

    @Query("select sd.copies, d.name as name, d.image as image, d.description as description from setmeal_dish sd " +
            "left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = :setmealId")
    Flux<DishItemVO> queryDishItemVoList(Long setmealId);

    @Query("SELECT category_id FROM dish WHERE id IN (:ids)")
    Flux<Long> queryCategoryIdByIds(List<Long> ids);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    @Query("SELECT COUNT(id) FROM dish " +
            "WHERE (:status IS NULL OR status = :status) " +
            "AND (:categoryId IS NULL OR category_id = :categoryId)")
    Mono<Integer> countByMap(Integer status, Long categoryId);

}
