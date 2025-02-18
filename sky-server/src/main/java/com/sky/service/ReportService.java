package com.sky.service;

import com.sky.result.Result;
import com.sky.vo.*;

import org.springframework.http.server.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ReportService {

    Mono<Result<TurnoverReportVO>> turnoverStatistics(LocalDate begin, LocalDate end);

    Mono<Result<UserReportVO>> userStatistics(LocalDate begin, LocalDate end);

    Mono<Result<OrderReportVO>> ordersStatistics(LocalDate begin, LocalDate end);

    Mono<Result<SalesTop10ReportVO>> top10(LocalDate begin, LocalDate end);

    void exportBusinessDate(ServerHttpResponse response);
}
