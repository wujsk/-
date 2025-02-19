package com.sky.repository;

import com.sky.entity.ShoppingCart;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface ShoppingCartRepository extends R2dbcRepository<ShoppingCart, Long> {

    /**
     * 批量保存购物车记录的默认方法实现
     *
     * @param shoppingCartList 购物车记录列表
     * @param databaseClient   数据库客户端
     * @return 表示操作完成的 Mono
     */
    default Mono<Void> saveBatch(List<ShoppingCart> shoppingCartList, DatabaseClient databaseClient) {
        if (shoppingCartList.isEmpty()) {
            return Mono.empty();
        }

        // 构建 SQL 语句
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO shopping_cart (name, dish_id, user_id, image, setmeal_id, amount, create_time, dish_flavor, number) VALUES ");
        for (int i = 0; i < shoppingCartList.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(",");
            }
            sqlBuilder.append("(:name").append(i).append(", :dishId").append(i).append(", :userId").append(i).append(", :image").append(i)
                    .append(", :setmealId").append(i).append(", :amount").append(i).append(", :createTime").append(i)
                    .append(", :dishFlavor").append(i).append(", :number").append(i).append(")");
        }

        // 构建执行规范并绑定参数
        DatabaseClient.GenericExecuteSpec executeSpec = databaseClient.sql(sqlBuilder.toString());
        for (int i = 0; i < shoppingCartList.size(); i++) {
            ShoppingCart cart = shoppingCartList.get(i);
            executeSpec = executeSpec
                    .bind("name" + i, cart.getName())
                    .bind("dishId" + i, cart.getDishId())
                    .bind("userId" + i, cart.getUserId())
                    .bind("image" + i, cart.getImage())
                    .bind("setmealId" + i, cart.getSetmealId())
                    .bind("amount" + i, cart.getAmount())
                    .bind("createTime" + i, cart.getCreateTime())
                    .bind("dishFlavor" + i, cart.getDishFlavor())
                    .bind("number" + i, cart.getNumber());
        }

        // 执行 SQL 并返回操作结果
        return executeSpec.fetch().rowsUpdated().then();
    }

    /**
     * 根据用户 ID 查询购物车列表
     */
    @Query("SELECT * FROM shopping_cart WHERE user_id = :userId")
    Flux<ShoppingCart> list(Long userId);

    /**
     * 查询购物车项
     */
    @Query("SELECT * FROM shopping_cart " +
            "WHERE (:userId IS NULL OR user_id = :userId) " +
            "AND (:dishId IS NULL OR dish_id = :dishId) " +
            "AND (:setmealId IS NULL OR setmeal_id = :setmealId) " +
            "AND (:dishFlavor IS NULL OR :dishFlavor = '' OR dish_flavor = :dishFlavor)")
    Mono<ShoppingCart> queryShoppringCart(ShoppingCart shoppingCart);

    /**
     * 更新购物车项
     */
    @Modifying
    @Query("UPDATE shopping_cart " +
            "SET amount = COALESCE(:amount, amount), " +
            "number = COALESCE(:number, number) " +
            "WHERE (:userId IS NULL OR user_id = :userId) " +
            "AND (:dishId IS NULL OR dish_id = :dishId) " +
            "AND (:setmealId IS NULL OR setmeal_id = :setmealId)")
    Mono<Void> update(ShoppingCart shoppingCart);

    /**
     * 删除购物车项
     */
    @Modifying
    @Query("DELETE FROM shopping_cart " +
            "WHERE (:userId IS NULL OR user_id = :userId) " +
            "AND (:id IS NULL OR id = :id)")
    Mono<Void> delete(ShoppingCart shoppingCart);
}
