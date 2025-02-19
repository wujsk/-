package com.sky.repository;

import com.sky.dto.OrderReportDTO;
import com.sky.dto.OrderSummaryDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends R2dbcRepository<Orders, Long> {

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Query("select * from orders where number = :orderNumber")
    Mono<Orders> getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    @Query("UPDATE orders " +
            "SET cancel_reason = COALESCE(:cancelReason, cancel_reason), " +
            "rejection_reason = COALESCE(:rejectionReason, rejection_reason), " +
            "cancel_time = COALESCE(:cancelTime, cancel_time), " +
            "pay_status = COALESCE(:payStatus, pay_status), " +
            "pay_method = COALESCE(:payMethod, pay_method), " +
            "checkout_time = COALESCE(:checkoutTime, checkout_time), " +
            "status = COALESCE(:status, status), " +
            "delivery_time = COALESCE(:deliveryTime, delivery_time) " +
            "WHERE id = :id")
    Mono<Integer> update(Orders orders);

    /**
     * 条件查询订单列表
     * @param ordersPageQueryDTO
     */
    @Query("SELECT * FROM orders " +
            "WHERE (:number IS NULL OR number LIKE CONCAT('%', :number, '%')) " +
            "AND (:phone IS NULL OR phone LIKE CONCAT('%', :phone, '%')) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:userId IS NULL OR user_id = :userId) " +
            "AND (:beginTime IS NULL OR order_time >= :beginTime) " +
            "AND (:endTime IS NULL OR order_time <= :endTime) " +
            "ORDER BY order_time DESC")
    Flux<Orders> list(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据 ID 查询订单
     * @param id
     */
    @Query("select * from orders where id = :id")
    Mono<Orders> queryById(Long id);

    /**
     * 统计订单状态
     */
    @Query("select status from orders where status = 2 or status = 3 or status = 4")
    Flux<Integer> statistics();

    /**
     * 批量更新订单信息
     * @param ordersList
     */
    default Mono<Void> updateBatch(List<Orders> ordersList, DatabaseClient databaseClient) {
        if (ordersList.isEmpty()) {
            return Mono.empty();
        }

        StringBuilder sqlBuilder = new StringBuilder();
        for (int i = 0; i < ordersList.size(); i++) {
            if (i > 0) {
                sqlBuilder.append("; ");
            }
            sqlBuilder.append("UPDATE orders SET ");
            sqlBuilder.append("cancel_reason = COALESCE(:cancelReason").append(i).append(", cancel_reason), ");
            sqlBuilder.append("rejection_reason = COALESCE(:rejectionReason").append(i).append(", rejection_reason), ");
            sqlBuilder.append("cancel_time = COALESCE(:cancelTime").append(i).append(", cancel_time), ");
            sqlBuilder.append("pay_status = COALESCE(:payStatus").append(i).append(", pay_status), ");
            sqlBuilder.append("pay_method = COALESCE(:payMethod").append(i).append(", pay_method), ");
            sqlBuilder.append("checkout_time = COALESCE(:checkoutTime").append(i).append(", checkout_time), ");
            sqlBuilder.append("status = COALESCE(:status").append(i).append(", status), ");
            sqlBuilder.append("delivery_time = COALESCE(:deliveryTime").append(i).append(", delivery_time) ");
            sqlBuilder.append("WHERE id = :id").append(i);
        }

        DatabaseClient.GenericExecuteSpec executeSpec = databaseClient.sql(sqlBuilder.toString());
        for (int i = 0; i < ordersList.size(); i++) {
            Orders order = ordersList.get(i);
            executeSpec = executeSpec.bind("cancelReason" + i, order.getCancelReason())
                    .bind("rejectionReason" + i, order.getRejectionReason())
                    .bind("cancelTime" + i, order.getCancelTime())
                    .bind("payStatus" + i, order.getPayStatus())
                    .bind("payMethod" + i, order.getPayMethod())
                    .bind("checkoutTime" + i, order.getCheckoutTime())
                    .bind("status" + i, order.getStatus())
                    .bind("deliveryTime" + i, order.getDeliveryTime())
                    .bind("id" + i, order.getId());
        }

        return executeSpec.fetch().rowsUpdated().then();
    }

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @param status
     * @return
     */
    @Query("SELECT DATE(order_time) as date, SUM(amount) as amount " +
            "FROM orders " +
            "WHERE (order_time BETWEEN :begin AND :end) " +
            "AND status = :status " +
            "GROUP BY DATE(order_time) " +
            "ORDER BY DATE(order_time)")
    Flux<OrderSummaryDTO> querySumCountByStatusAndTime(LocalDateTime begin, LocalDateTime end, Integer status);


    /**
     * 订单统计
     * @param params
     * @return
     */
    @Query("SELECT date(order_time) as date, " +
            "COUNT(CASE WHEN status = :#{#params['completed']} THEN 1 END) as completed, " +
            "COUNT(*) as total " +
            "FROM orders " +
            "WHERE (order_time BETWEEN :#{#params['begin']} AND :#{#params['end']}) " +
            "GROUP BY date(order_time) " +
            "ORDER BY date(order_time) ASC")
    Flux<OrderReportDTO> getStatistics(Map<String, Object> params);

    @Query("SELECT COUNT(*) FROM orders " +
            "WHERE (:begin IS NULL OR order_time >= :begin) " +
            "AND (:end IS NULL OR order_time <= :end) " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Integer> countByMap(LocalDateTime begin, LocalDateTime end, Integer status);

    @Query("SELECT SUM(amount) FROM orders " +
            "WHERE (:begin IS NULL OR :end IS NULL OR order_time BETWEEN :begin AND :end) " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Double> sumByMap(LocalDateTime begin, LocalDateTime end, Integer status);

}
