package com.sky.service.impl;

import cn.hutool.core.util.StrUtil;
import com.sky.dto.NewUserStatisticsDTO;
import com.sky.dto.OrderReportDTO;
import com.sky.dto.OrderSummaryDTO;
import com.sky.entity.Orders;
import com.sky.repository.OrderDetailRepository;
import com.sky.repository.OrderRepository;
import com.sky.repository.UserRepository;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.dev33.satoken.SaManager.log;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-20 11:10
 **/
@Service
public class ReportServiceImpl implements ReportService {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private OrderDetailRepository orderDetailRepository;

    @Resource
    private WorkspaceService workspaceService;

    public List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.isEqual(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(begin);
        return dateList;
    }

    @Override
    public Mono<Result<TurnoverReportVO>> turnoverStatistics(LocalDate begin, LocalDate end) {
        // 计算dateList
        List<LocalDate> dateList = getDateList(begin, end);
        LocalDateTime startTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        return orderRepository.querySumCountByStatusAndTime(startTime, endTime,  Orders.COMPLETED)
                .collectMap(OrderSummaryDTO::getDate, OrderSummaryDTO::getAmount)
                .flatMap(amountSum -> {
                    List<BigDecimal> amount = new ArrayList<>();
                    for (LocalDate date : dateList) {
                        Date dt = Date.valueOf(date);
                        amount.add(amountSum.getOrDefault(dt, BigDecimal.ZERO));
                    }
                    String dateListStr = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));
                    String turnoverListStr = amount.stream().map(BigDecimal::toString).collect(Collectors.joining(","));
                    return Mono.just(Result.success(TurnoverReportVO.builder()
                            .dateList(dateListStr)
                            .turnoverList(turnoverListStr)
                            .build()));
                })
                .onErrorResume(e -> {
                    log.error("获取营业额失败", e);
                    return Mono.error(new RuntimeException("获取营业额失败"));
                });
    }

    @Override
    public Mono<Result<UserReportVO>> userStatistics(LocalDate begin, LocalDate end) {
        // 将日期存到dateList中
        List<LocalDate> dateList = getDateList(begin, end);
        LocalDateTime startTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 将开始前所有的用户加起来
        return userRepository.queryTotalUserBeforeBeginTime(begin)
                .flatMap(totalUserBeforeBeginTime -> {
                    // 找到当天注册的所有用户（新用户）
                    return userRepository.queryNewUserBetweenTime(startTime, endTime)
                            .collectMap(NewUserStatisticsDTO::getDate, NewUserStatisticsDTO::getNum)
                            .flatMap(dateMapMap -> {
                                Long beforeNum = totalUserBeforeBeginTime;
                                List<Long> newUser = new ArrayList<>();
                                List<Long> totalUser = new ArrayList<>();
                                for (LocalDate date : dateList) {
                                    Date time = Date.valueOf(date);
                                    Long num = dateMapMap.getOrDefault(time, 0L);
                                    newUser.add(num);
                                    beforeNum += num;
                                    totalUser.add(beforeNum);
                                }
                                String dateListStr = StrUtil.join(",", dateList);
                                String newUserListStr = StrUtil.join(",", newUser);
                                String totalUserListStr = StrUtil.join(",", totalUser);
                                return Mono.just(Result.success(UserReportVO.builder()
                                        .dateList(dateListStr)
                                        .newUserList(newUserListStr)
                                        .totalUserList(totalUserListStr)
                                        .build()));
                            });
                });
    }

    @Override
    public Mono<Result<OrderReportVO>> ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        Map<String, Object> params = new HashMap<>();
        params.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
        params.put("end", LocalDateTime.of(end, LocalTime.MAX));
        params.put("completed", Orders.COMPLETED);
        return orderRepository.getStatistics(params)
                .collectMap(OrderReportDTO::getDate, dto -> dto)
                .flatMap(map -> {
                    List<Integer> orderCountList = new ArrayList<>();
                    List<Integer> validOrderCountList = new ArrayList<>();
                    int totalOrderCount = 0;
                    int validOrderCount = 0;

                    for (LocalDate date : dateList) {
                        Date time = Date.valueOf(date);
                        if (map.containsKey(time)) {
                            OrderReportDTO dto = map.get(time);
                            Long total = dto.getTotal();
                            Long validOrder = dto.getCompleted();
                            int var1 = total.intValue();
                            int var2 = validOrder.intValue();
                            validOrderCountList.add(var2);
                            orderCountList.add(var1);
                            validOrderCount += var2;
                            totalOrderCount += var1;
                        } else {
                            validOrderCountList.add(0);
                            orderCountList.add(0);
                        }
                    }

                    String dateListStr = StrUtil.join(",", dateList);
                    String validOrderCountListStr = StrUtil.join(",", validOrderCountList);
                    String orderCountListStr = StrUtil.join(",", orderCountList);

                    double orderCompletionRate = totalOrderCount == 0 ? 0 : validOrderCount * 1.0 / totalOrderCount;

                    return Mono.just(Result.success(OrderReportVO.builder()
                            .dateList(dateListStr)
                            .validOrderCountList(validOrderCountListStr)
                            .orderCountList(orderCountListStr)
                            .validOrderCount(validOrderCount)
                            .totalOrderCount(totalOrderCount)
                            .orderCompletionRate(orderCompletionRate)
                            .build()));
                })
                .onErrorResume(e -> {
                    log.error("获取订单报表失败", e);
                    return Mono.error(new RuntimeException("获取订单报表失败"));
                });
    }

    @Override
    public Mono<Result<SalesTop10ReportVO>> top10(LocalDate begin, LocalDate end) {
        return orderDetailRepository.queryTop10(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX))
                .collectList()
                .flatMap(salesTop10ReportDTOList -> {
                    List<String> nameList = new ArrayList<>();
                    List<Long> numberList = new ArrayList<>();
                    salesTop10ReportDTOList.forEach(salesTop10ReportDTO -> {
                        nameList.add(salesTop10ReportDTO.getName());
                        numberList.add(salesTop10ReportDTO.getSaleCount());
                    });
                    return Mono.just(Result.success(SalesTop10ReportVO.builder()
                            .nameList(StrUtil.join(",", nameList))
                            .numberList(StrUtil.join(",", numberList))
                            .build()));
                })
                .onErrorResume(e -> {
                    log.error("获取top10报表失败", e);
                    return Mono.error(new RuntimeException("获取top10报表失败"));
                });
    }

    @Override
    public void exportBusinessDate(ServerHttpResponse response) {
        OutputStream outputStream = null;
        XSSFWorkbook excel = null;
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        // 读取数据
        BusinessDataVO businessData = workspaceService.
                getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        // 读取模板文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            // 基于模板文件创建一个新的excel文件
            excel = new XSSFWorkbook(in);
            // 填充时间
            // Sheet注意大小写
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            // row、cell索引是从0开始的
            sheet1.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            // 获得第四行
            XSSFRow row3 = sheet1.getRow(3);
            row3.getCell(2).setCellValue(businessData.getTurnover());
            row3.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row3.getCell(6).setCellValue(businessData.getNewUsers());
            // 获得第五行
            XSSFRow row4 = sheet1.getRow(4);
            row4.getCell(2).setCellValue(businessData.getValidOrderCount());
            row4.getCell(4).setCellValue(businessData.getUnitPrice());

            // 通过输出流，将excel文件下载到客户端浏览器
            outputStream = response.getBody();
            excel.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (!Objects.isNull(outputStream)) {
                    outputStream.close();
                }
                if (Objects.isNull(excel)) {
                    excel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
