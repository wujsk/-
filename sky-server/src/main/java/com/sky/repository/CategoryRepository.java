package com.sky.repository;

import com.sky.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CategoryRepository extends R2dbcRepository<Category, Long> {

    /**
     * 根据名称和类型分页查询
     * @param name
     * @param type
     * @return
     */
    Flux<Category> findAllByNameIsLikeAndType(String name, Integer type, Pageable pageable);

    /**
     * 根据名称和类型查询分类数量
     * @param name
     * @param type
     * @return
     */
    Mono<Long> countByNameIsLikeAndType(String name, Integer type);

    /**
     * 根据id修改分类
     * @param category
     */
    @Modifying
    @Query("UPDATE category SET " +
            "type = CASE WHEN :#{#category.type} IS NULL THEN type ELSE :#{#category.type} END, " +
            "name = CASE WHEN :#{#category.name} IS NULL OR :#{#category.name} = '' THEN name ELSE :#{#category.name} END, " +
            "sort = CASE WHEN :#{#category.sort} IS NULL THEN sort ELSE :#{#category.sort} END, " +
            "status = CASE WHEN :#{#category.status} IS NULL THEN status ELSE :#{#category.status} END, " +
            "update_time = CASE WHEN :#{#category.updateTime} IS NULL THEN update_time ELSE :#{#category.updateTime} END, " +
            "update_user = CASE WHEN :#{#category.updateUser} IS NULL THEN update_user ELSE :#{#category.updateUser} END " +
            "WHERE id = :#{#category.id}")
    Mono<Integer> update(Category category);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    Flux<Category> findAllByType(Integer type);
}
