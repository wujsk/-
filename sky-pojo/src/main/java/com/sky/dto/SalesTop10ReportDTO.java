package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: cyy
 * @create: 2025-02-18 17:05
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesTop10ReportDTO {

    /**
     * 商品名称
     */
    private String name;

    /**
     * 销量
     */
    private Long saleCount;
}
