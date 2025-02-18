package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.SystemException;
import com.sky.repository.CategoryRepository;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealRepository;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private DishRepository dishRepository;

    @Resource
    private SetmealRepository setmealRepository;

    /**
     * 新增分类
     * @param categoryDTO
     */
    @Override
    public Mono<Result<String>> save(CategoryDTO categoryDTO) {
        return Mono.just(categoryDTO)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("分类信息不能为空")))
                .flatMap(dto -> {
                    Category category = new Category();
                    //属性拷贝
                    BeanUtils.copyProperties(categoryDTO, category);
                    //分类状态默认为禁用状态0
                    category.setStatus(StatusConstant.DISABLE);
                    category.setCreateTime(LocalDateTime.now());
                    category.setUpdateTime(LocalDateTime.now());
                    category.setCreateUser(StpUtil.getLoginIdAsLong());
                    category.setUpdateUser(StpUtil.getLoginIdAsLong());
                    return categoryRepository.save(category);
                })
                .flatMap(category -> Mono.just(Result.success("新增分类成功")))
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    if (e instanceof DuplicateKeyException) {
                        return Mono.just(Result.error("分类名称已存在"));
                    }
                    return Mono.just(Result.error("新增分类失败"));
                });
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public Mono<Result<PageResult>> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        int page = categoryPageQueryDTO.getPage();
        int pageSize = categoryPageQueryDTO.getPageSize();
        String name = categoryPageQueryDTO.getName();
        Integer type = categoryPageQueryDTO.getType();
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        Pageable pageable = PageRequest.of( page- 1, pageSize);
        name = StringUtils.isBlank(name) ? "%" : "%" + name + "%";
        return categoryRepository.findAllByNameIsLikeAndType(name, type, pageable)
                .collectList()
                .zipWith(categoryRepository.countByNameIsLikeAndType(name, type))
                .flatMap(tuple -> {
                    PageResult pageResult = new PageResult();
                    pageResult.setRecords(tuple.getT1());
                    pageResult.setTotal(tuple.getT2());
                    return Mono.just(Result.success(pageResult));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

    /**
     * 根据id删除分类
     * @param id
     */
    @Override
    public Mono<Result<String>> deleteById(Long id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("分类id不能为空"));
        }
        return dishRepository.countByCategoryId(id)
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.just(Result.<String>error(MessageConstant.CATEGORY_BE_RELATED_BY_DISH));
                    }
                    return setmealRepository.countByCategoryId(id)
                            .flatMap(count1 -> {
                                if (count1 > 0) {
                                    return Mono.just(Result.<String>error(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL));
                                }
                                return categoryRepository.deleteById(id)
                                        .then(Mono.defer(() -> Mono.just(Result.success("删除成功"))));
                            });
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @Override
    public Mono<Result<String>> update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);
        return updateCategory(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    @Override
    public Mono<Result<String>> startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        return updateCategory(category);
    }

    private Mono<Result<String>> updateCategory(Category category) {
        return categoryRepository.update(category)
                .flatMap(count -> {
                    if (count < 1) {
                        return Mono.just(Result.<String>error("操作失败"));
                    }
                    return Mono.just(Result.success("操作成功"));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @Override
    public Mono<Result<List<Category>>> list(Integer type) {
        return categoryRepository.findAllByType(type)
                .collectList()
                .flatMap(categories -> {
                    return Mono.just(Result.success(categories));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }
}
