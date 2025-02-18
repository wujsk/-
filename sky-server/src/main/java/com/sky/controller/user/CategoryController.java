package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

/**
 * @author: cyy
 * @create: 2025-02-18 13:48
 **/
@RestController("userCategoryController")
@RequestMapping("/user/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Mono<Result<List<Category>>> list(Integer type) {
        return categoryService.list(type);
    }
}
