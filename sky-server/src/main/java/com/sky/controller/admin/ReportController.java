package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * @author: cyy
 * @create: 2025-02-18 13:43
 **/
@RestController
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    public Mono<Result<TurnoverReportVO>> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                             @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额统计{},{}", begin, end);
        return reportService.turnoverStatistics(begin, end);
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    public Mono<Result<UserReportVO>> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户统计{},{}", begin, end);
        return reportService.userStatistics(begin, end);
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    public Mono<Result<OrderReportVO>> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单统计{},{}", begin, end);
        return reportService.ordersStatistics(begin, end);
    }

    /**
     * 销售前十
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    public Mono<Result<SalesTop10ReportVO>> top10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return reportService.top10(begin, end);
    }

    /**
     * 导出报表
     * @param response
     */
    @GetMapping("/export")
    public void export(ServerHttpResponse response) {
        reportService.exportBusinessDate(response);
    }
}
