package com.sky.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * @author: cyy
 * @create: 2025-02-18 09:47
 **/
@Configuration
@EnableR2dbcRepositories(basePackages = "com.sky.repository")
public class R2dbcConfiguration {
}
