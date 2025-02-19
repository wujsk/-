package com.sky.repository;

import com.sky.dto.SalesTop10ReportDTO;
import com.sky.entity.OrderDetail;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends R2dbcRepository <OrderDetail, Long> {

    /**
     * 批量保存订单详情
     * @param orderDetailList 订单详情列表
     * @param databaseClient 数据库客户端
     * @return Mono<Void> 表示操作完成的信号
     */
    default Mono<Void> saveBatch(List<OrderDetail> orderDetailList, DatabaseClient databaseClient) {
        if (orderDetailList.isEmpty()) {
            return Mono.empty();
        }

        StringBuilder sql = new StringBuilder("INSERT INTO order_detail (number, order_id, name, dish_id, setmeal_id, dish_flavor, amount, image) VALUES ");
        for (int i = 0; i < orderDetailList.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("(:number").append(i).append(", :orderId").append(i).append(", :name").append(i).append(", :dishId").append(i).append(", :setmealId").append(i).append(", :dishFlavor").append(i).append(", :amount").append(i).append(", :image").append(i).append(")");
        }

        DatabaseClient.GenericExecuteSpec executeSpec = databaseClient.sql(sql.toString());

        for (int i = 0; i < orderDetailList.size(); i++) {
            OrderDetail orderDetail = orderDetailList.get(i);
            executeSpec = executeSpec
                    .bind("number" + i, orderDetail.getNumber())
                    .bind("orderId" + i, orderDetail.getOrderId())
                    .bind("name" + i, orderDetail.getName())
                    .bind("dishId" + i, orderDetail.getDishId())
                    .bind("setmealId" + i, orderDetail.getSetmealId())
                    .bind("dishFlavor" + i, orderDetail.getDishFlavor())
                    .bind("amount" + i, orderDetail.getAmount())
                    .bind("image" + i, orderDetail.getImage());
        }

        return executeSpec.fetch().rowsUpdated().then();
    }

    /**
     * 根据订单 ID 查询订单详情
     * @param orderId
     * @return
     */
    @Query("SELECT * FROM order_detail WHERE order_id = :orderId")
    Flux<OrderDetail> queryByOrderId(Long orderId);

    /**
     * 根据订单 ID 删除订单详情
     * @param orderId
     * @return
     */
    @Query("DELETE FROM order_detail WHERE order_id = :orderId")
    Mono<Integer> deleteByOrderId(Long orderId);

    @Query("SELECT name, COUNT(*) as sale_count " +
            "FROM order_detail " +
            "WHERE create_time BETWEEN :begin AND :end " +
            "GROUP BY name " +
            "ORDER BY sale_count DESC " +
            "LIMIT 10")
    Flux<SalesTop10ReportDTO> queryTop10(LocalDateTime begin, LocalDateTime end);
}
