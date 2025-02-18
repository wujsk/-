package com.sky.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author: cyy
 * @create: 2025-02-18 17:37
 **/
@Data
public class OrderSummaryDTO {

    private Date date;

    private BigDecimal amount;
}
