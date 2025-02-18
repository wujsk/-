package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:43
 **/
@RestController("adminCategoryController")
@RequestMapping("/admin/category")
@Slf4j
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    public Mono<Result<String>> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}", categoryDTO);
        return categoryService.save(categoryDTO);
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Mono<Result<PageResult>> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);
        return categoryService.pageQuery(categoryPageQueryDTO);
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    public Mono<Result<String>> deleteById(Long id){
        return categoryService.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    public Mono<Result<String>> update(@RequestBody CategoryDTO categoryDTO){
        return categoryService.update(categoryDTO);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Mono<Result<String>> startOrStop(@PathVariable("status") Integer status, Long id){
        return categoryService.startOrStop(status,id);
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Mono<Result<List<Category>>> list(Integer type){
        return categoryService.list(type);
    }
}
