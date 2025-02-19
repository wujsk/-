package com.sky.repository;

import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface SetmealRepository extends R2dbcRepository<Setmeal, Long> {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Query("select count(id) from setmeal where category_id = :categoryId")
    Mono<Long> countByCategoryId(Long id);

    @Query("SELECT s.*, c.name AS category_name FROM setmeal s " +
            "LEFT JOIN category c ON s.category_id = c.id " +
            "WHERE s.category_id = :categoryId AND s.status = :status " +
            "ORDER BY s.create_time DESC")
    Flux<SetmealVO> querySetmealListByCategoryIdAndStatus(Setmeal setmeal);

    @Query("SELECT s.*, c.name AS category_name FROM setmeal s " +
            "LEFT JOIN category c ON s.category_id = c.id " +
            "WHERE (:name IS NULL OR s.name LIKE concat('%', :name, '%')) " +
            "AND (:categoryId IS NULL OR s.category_id = :categoryId) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "ORDER BY s.create_time DESC " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<SetmealVO> querySetmealList(String name, Long categoryId, Integer status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM setmeal s " +
            "LEFT JOIN category c ON s.category_id = c.id " +
            "WHERE (:name IS NULL OR s.name LIKE concat('%', :name, '%')) " +
            "AND (:categoryId IS NULL OR s.category_id = :categoryId) " +
            "AND (:status IS NULL OR s.status = :status)")
    Mono<Long> countSetmeals(String name, Long categoryId, Integer status);

    @Query("select * from setmeal where id = :id")
    Mono<SetmealVO> querySetmealById(Long id);

    @Modifying
    @Query("UPDATE setmeal " +
            "SET name = CASE WHEN :name IS NULL OR :name = '' THEN name ELSE :name END, " +
            "description = CASE WHEN :description IS NULL OR :description = '' THEN description ELSE :description END, " +
            "price = CASE WHEN :price IS NULL THEN price ELSE :price END, " +
            "pic = CASE WHEN :pic IS NULL OR :pic = '' THEN pic ELSE :pic END, " +
            "status = CASE WHEN :status IS NULL THEN status ELSE :status END, " +
            "category_id = CASE WHEN :categoryId IS NULL THEN category_id ELSE :categoryId END " +
            "WHERE id = :id")
    Mono<Integer> updateSetmeal(Setmeal setmeal);

    @Modifying
    @Query("DELETE FROM setmeal WHERE id IN (:ids)")
    Mono<Long> deleteSetmealByIds(List<Long> ids);

    @Query("SELECT COUNT(*) FROM setmeal s " +
            "WHERE s.status = :status AND s.id IN (:ids)")
    Mono<Long> countStatus(Integer status, List<Long> ids);

    @Query("SELECT category_id FROM setmeal WHERE id = :id")
    Mono<Long> queryCategoryIdById(Long id);

    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    @Query("SELECT COUNT(id) FROM setmeal " +
            "WHERE (:status IS NULL OR status = :status) " +
            "AND (:categoryId IS NULL OR category_id = :categoryId)")
    Mono<Integer> countByMap(Integer status, Long categoryId);
}
