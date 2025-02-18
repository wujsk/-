package com.sky.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author: cyy
 * @create: 2025-02-18 17:22
 **/
@Data
public class OrderReportDTO {

    /**
     * 时间
     */
    private Date date;

    /**
     * 已完成数
     */
    private Long completed;

    /**
     * 总订单
     */
    private Long total;
}
