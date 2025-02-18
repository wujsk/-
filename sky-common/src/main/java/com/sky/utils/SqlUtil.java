package com.sky.utils;

/**
 * @author: cyy
 * @create: 2025-02-18 10:36
 **/
public class SqlUtil {

    /**
     * 拼接模糊字段
     * @param column
     * @return
     */
    public static String getLikeSql(String column){
        return "%" + column + "%";
    }
}
