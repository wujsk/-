package com.sky.repository;

import com.sky.entity.ShoppingCart;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingCartRepository {

    void save(ShoppingCart shoppingCart);

    void saveBatch(@Param("shoppingCartList") List<ShoppingCart> shoppingCartList);

    List<ShoppingCart> list(Long userId);

    ShoppingCart queryShoppringCart(ShoppingCart shoppingCart);

    void update(ShoppingCart shoppingCart);

    void delete(ShoppingCart shoppingCart);
}
