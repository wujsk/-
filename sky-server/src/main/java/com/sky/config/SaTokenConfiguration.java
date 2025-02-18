package com.sky.config;

import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.spring.SaTokenContextForSpringReactor;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.sky.result.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;

import javax.swing.*;

/**
 * @author: cyy
 * @create: 2025-02-17 20:37
 **/
@Configuration
public class SaTokenConfiguration {

    /**
     * 注册 [Sa-Token全局过滤器]
     */
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude(
                        "/favicon.ico",
                        "/admin/employee/login",
                        "/static/*",
                        "/*.html",
                        "/*.css",
                        "/*.js"
                )
                .setAuth(obj -> {
                    System.out.println("---------- sa全局认证");
                    StpUtil.checkLogin();
                })
                .setError(e -> {
                    System.out.println("---------- sa全局异常 ");
                    return Result.error(e.getMessage());
                });
    }

    // Sa-Token 整合 jwt (Simple 简单模式)
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
