package com.sky.repository;

import com.sky.dto.NewUserStatisticsDTO;
import com.sky.entity.User;
import org.springframework.cglib.core.Local;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 21:48
 **/
@Repository
public interface UserRepository extends R2dbcRepository <User, Long> {

    Mono<User> findUserByOpenid(String openid);

    @Query("select count(*) from user where create_time < :begin")
    Mono<Long> queryTotalUserBeforeBeginTime(LocalDate begin);

    @Query("select date(create_time) as date, count(*) as num " +
            "from user " +
            "where create_time between :begin and :end " +
            "group by date(create_time) " +
            "order by date(create_time) asc")
    Flux<NewUserStatisticsDTO> queryNewUserBetweenTime(LocalDateTime begin, LocalDateTime end);

    @Query("SELECT COUNT(*) FROM user " +
            "WHERE (:begin IS NULL OR :end IS NULL OR create_time BETWEEN :begin AND :end)")
    Mono<Integer> countByMap(LocalDateTime begin, LocalDateTime end);
}
