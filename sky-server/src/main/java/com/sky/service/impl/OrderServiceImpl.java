package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.repository.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private AddressBookRepository addressBookRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private ShoppingCartRepository shoppingCartRepository;

    @Resource
    private OrderDetailRepository orderDetailRepository;

    @Resource
    private WebSocketServer webSocketServer;

    @Resource
    private WebClient webClient;

    @Resource
    private DatabaseClient databaseClient;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Override
    @Transactional
    public Mono<Result<OrderSubmitVO>> submit(OrdersSubmitDTO submitDTO) {
        if (submitDTO == null) {
            return Mono.just(Result.error("下单失败"));
        }
        Long userId = StpUtil.getLoginIdAsLong();
        Long addressBookId = submitDTO.getAddressBookId();
        if (addressBookId == null) {
            return Mono.error(new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL));
        }

        return addressBookRepository.findById(addressBookId)
                .switchIfEmpty(Mono.error(new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL)))
                .flatMap(addressBook -> {
                    return shoppingCartRepository.list(userId)
                            .collectList()
                            .flatMap(shoppingCartList -> {
                                if (CollectionUtil.isEmpty(shoppingCartList)) {
                                    return Mono.error(new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL));
                                }
                                return checkOutOfRange(addressBook.getProvinceName() + addressBook.getCityName()
                                        + addressBook.getDistrictName() + addressBook.getDetail())
                                        .then(Mono.fromSupplier(() -> System.currentTimeMillis()))
                                        .flatMap(number -> {
                                            String address = addressBook.getProvinceName() + addressBook.getCityName()
                                                    + addressBook.getDistrictName() + addressBook.getDetail();
                                            return userRepository.findById(userId)
                                                    .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                                                    .flatMap(user -> {
                                                        Orders orders = BeanUtil.copyProperties(submitDTO, Orders.class);
                                                        LocalDateTime now = LocalDateTime.now();
                                                        orders.setNumber(String.valueOf(number));
                                                        orders.setStatus(Orders.PENDING_PAYMENT);
                                                        orders.setUserId(userId);
                                                        orders.setOrderTime(now);
                                                        orders.setPayStatus(Orders.UN_PAID);
                                                        orders.setUserName(user.getName());
                                                        orders.setPhone(user.getPhone());
                                                        orders.setAddress(address);
                                                        orders.setConsignee(addressBook.getConsignee());

                                                        return orderRepository.save(orders)
                                                                .flatMap(savedOrder -> {
                                                                    Long orderId = savedOrder.getId();
                                                                    ShoppingCart shoppingCart1 = new ShoppingCart();
                                                                    shoppingCart1.setUserId(userId);
                                                                    return shoppingCartRepository.delete(shoppingCart1)
                                                                            .thenMany(Flux.fromIterable(shoppingCartList)
                                                                                    .map(shoppingCart -> {
                                                                                        OrderDetail orderDetail = BeanUtil.copyProperties(shoppingCart, OrderDetail.class);
                                                                                        orderDetail.setOrderId(orderId);
                                                                                        return orderDetail;
                                                                                    })
                                                                                    .flatMap(orderDetailRepository::save))
                                                                            .then(Mono.just(savedOrder));
                                                                })
                                                                .map(savedOrder -> {
                                                                    OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                                                                            .id(savedOrder.getId())
                                                                            .orderNumber(String.valueOf(number))
                                                                            .orderAmount(submitDTO.getAmount())
                                                                            .orderTime(now)
                                                                            .build();
                                                                    return Result.success(orderSubmitVO);
                                                                });
                                                    });
                                        });
                            });
                });
    }

    public Mono<Result<OrderPaymentVO>> payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = StpUtil.getLoginIdAsLong();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    JSONObject jsonObject = new JSONObject();
                    if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
                        return Mono.error(new OrderBusinessException("该订单已支付"));
                    }
                    OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
                    vo.setPackageStr(jsonObject.getString("package"));
                    return Mono.just(Result.success(vo));
                });
    }

    @Override
    public Mono<Void> paySuccess(String outTradeNo) {
        return orderRepository.getByNumber(outTradeNo)
                .flatMap(ordersDB -> {
                    Orders orders = Orders.builder()
                            .id(ordersDB.getId())
                            .status(Orders.TO_BE_CONFIRMED)
                            .payStatus(Orders.PAID)
                            .checkoutTime(LocalDateTime.now())
                            .build();
                    return orderRepository.update(orders)
                            .then(Mono.fromRunnable(() -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("type", 1);
                                map.put("orderId", outTradeNo);
                                map.put("content", "订单号：" + outTradeNo);
                                String json = JSON.toJSONString(map);
                                webSocketServer.sendAllClient(json);
                            }));
                });
    }

    @Override
    public Mono<Result<PageResult>> pageList(OrdersPageQueryDTO ordersPageQueryDTO) {
        int page = ordersPageQueryDTO.getPage();
        int pageSize = ordersPageQueryDTO.getPageSize();
        String number = ordersPageQueryDTO.getNumber();
        String phone = ordersPageQueryDTO.getPhone();
        Integer status = ordersPageQueryDTO.getStatus();
        LocalDateTime beginTime = ordersPageQueryDTO.getBeginTime();
        LocalDateTime endTime = ordersPageQueryDTO.getEndTime();
        Long userId = ordersPageQueryDTO.getUserId();

        // 检查页码和页大小是否合法
        if (page < 1 || pageSize < 1) {
            return Mono.error(new IllegalArgumentException("页码和页大小必须大于0"));
        }
        // 设置用户ID
        ordersPageQueryDTO.setUserId(StpUtil.getLoginIdAsLong());

        // 创建 Pageable 对象
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 查询订单列表
        Mono<List<Orders>> ordersListMono = orderRepository.queryOrderList(number, phone, status, beginTime, endTime, userId, pageable)
                .collectList();

        // 查询订单总数
        Mono<Long> countMono = orderRepository.queryOrderCount(number, phone, status, beginTime, endTime, userId);

        // 合并订单列表和总数的查询结果
        return Mono.zip(ordersListMono, countMono)
                .flatMap(tuple -> {
                    List<Orders> ordersList = tuple.getT1();
                    long total = tuple.getT2();

                    if (ordersList.isEmpty()) {
                        return Mono.just(Result.success(new PageResult(0, Collections.emptyList())));
                    }

                    // 将 Orders 转换为 OrderVO
                    List<OrderVO> orderVOList = ordersList.stream()
                            .map(order -> BeanUtil.copyProperties(order, OrderVO.class))
                            .collect(Collectors.toList());

                    // 处理每个订单的详情
                    Mono<Void> populateOrderDetailsMono = Mono.when(orderVOList.stream()
                            .map(orderVO -> {
                                Long orderId = orderVO.getId();
                                return orderDetailRepository.queryByOrderId(orderId).collectList()
                                        .doOnNext(orderDetailList -> {
                                            List<String> orderDishList = orderDetailList.stream()
                                                    .map(x -> x.getName() + "*" + x.getNumber() + ";")
                                                    .collect(Collectors.toList());
                                            orderVO.setOrderDishes(String.join("", orderDishList));
                                            orderVO.setOrderDetailList(orderDetailList);
                                        });
                            })
                            .toArray(Mono[]::new));

                    return populateOrderDetailsMono.then(Mono.just(Result.success(new PageResult(total, orderVOList))));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new RuntimeException("系统错误，请稍后重试"));
                });
    }

    @Override
    public Mono<Result<OrderVO>> getOrderDetail(Long id) {
        return orderRepository.queryById(id)
                .switchIfEmpty(Mono.error(new BaseException("暂无该订单信息")))
                .flatMap(orders -> {
                    OrderVO orderVO = BeanUtil.copyProperties(orders, OrderVO.class);
                    return orderDetailRepository.queryByOrderId(id)
                            .collectList()
                            .map(orderDetailList -> {
                                orderVO.setOrderDetailList(orderDetailList);
                                return Result.success(orderVO);
                            });
                });
    }

    @Override
    public Mono<Result<String>> adminCancel(OrdersCancelDTO ordersCancelDTO) {
        return orderRepository.queryById(ordersCancelDTO.getId())
                .switchIfEmpty(Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)))
                .flatMap(ordersDB -> {
                    Orders orders = new Orders();
                    orders.setId(ordersCancelDTO.getId());
                    orders.setStatus(Orders.CANCELLED);
                    orders.setCancelReason(ordersCancelDTO.getCancelReason());
                    orders.setCancelTime(LocalDateTime.now());
                    return orderRepository.update(orders)
                            .then(Mono.just(Result.success("修改成功")));
                });
    }

    @Override
    @Transactional
    public Mono<Result<String>> cancel(Orders orders) {
        Long orderId = orders.getId();
        if (orderId == null) {
            return Mono.error(new BaseException("请选择你要取消的订单"));
        }
        return orderRepository.queryById(orderId)
                .switchIfEmpty(Mono.error(new BaseException("暂无该订单信息")))
                .flatMap(ordersById -> {
                    if (orders.getStatus() > 2) {
                        return Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR));
                    }
                    orders.setCancelTime(LocalDateTime.now());
                    orders.setStatus(Orders.CANCELLED);
                    orders.setCancelReason("用户取消");
                    return orderRepository.update(orders)
                            .then(Mono.just(Result.success("取消成功")));
                });
    }

    @Override
    @Transactional
    public Mono<Result<String>> repetition(Long id) {
        return orderRepository.queryById(id)
                .switchIfEmpty(Mono.error(new BaseException("再来一单失败")))
                .flatMap(orders -> {
                    return orderDetailRepository.queryByOrderId(id)
                            .collectList()
                            .flatMap(orderDetailList -> {
                                List<ShoppingCart> shoppingCartList = orderDetailList.stream()
                                        .map(x -> {
                                            ShoppingCart shoppingCart = new ShoppingCart();
                                            BeanUtils.copyProperties(x, shoppingCart, "id");
                                            shoppingCart.setUserId(StpUtil.getLoginIdAsLong());
                                            shoppingCart.setCreateTime(LocalDateTime.now());
                                            return shoppingCart;
                                        })
                                        .collect(Collectors.toList());
                                return shoppingCartRepository.saveBatch(shoppingCartList, databaseClient)
                                        .then(Mono.just(Result.success("操作成功")));
                            });
                });
    }

    @Override
    public Mono<Result<OrderStatisticsVO>> statistics() {
        return orderRepository.statistics()
                .collectList()
                .flatMap(statusList -> {
                    Integer toBeConfirmed = 0;
                    Integer confirmed = 0;
                    Integer deliveryInProgress = 0;
                    for (Integer status : statusList) {
                        switch (status) {
                            case 2:
                                toBeConfirmed++;
                                break;
                            case 3:
                                confirmed++;
                                break;
                            case 4:
                                deliveryInProgress++;
                        }
                    }
                    OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO(toBeConfirmed, confirmed, deliveryInProgress);
                    return Mono.just(Result.success(orderStatisticsVO));
                });
    }

    @Override
    public Mono<Result<String>> confirm(Orders orders) {
        if (orders.getId() == null) {
            return Mono.just(Result.error("请选择你要接单的单子"));
        }
        orders.setStatus(Orders.CONFIRMED);
        return orderRepository.update(orders)
                .then(Mono.just(Result.success()));
    }

    @Override
    public Mono<Result<String>> rejection(Orders orders) {
        if (orders.getId() == null) {
            return Mono.just(Result.error("请选择你要拒单的单子"));
        }
        if (StrUtil.isBlank(orders.getRejectionReason())) {
            return Mono.just(Result.error("请选择拒单原因"));
        }
        Long orderId = orders.getId();
        return orderRepository.queryById(orderId)
                .switchIfEmpty(Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)))
                .flatMap(orderDB -> {
                    if (!orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
                        return Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR));
                    }
                    orders.setStatus(Orders.CANCELLED);
                    orders.setPayStatus(Orders.REFUND);
                    orders.setCancelTime(LocalDateTime.now());
                    return orderRepository.update(orders)
                            .then(Mono.just(Result.success()));
                });
    }

    @Override
    public Mono<Result<String>> delivery(Long id) {
        if (id == null) {
            return Mono.just(Result.error("派单失败"));
        }
        return orderRepository.queryById(id)
                .switchIfEmpty(Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)))
                .flatMap(ordersDB -> {
                    if (!ordersDB.getStatus().equals(Orders.CONFIRMED)) {
                        return Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR));
                    }
                    Orders orders = Orders.builder()
                            .id(id)
                            .status(Orders.DELIVERY_IN_PROGRESS)
                            .build();
                    return orderRepository.update(orders)
                            .then(Mono.just(Result.success()));
                });
    }

    @Override
    public Mono<Result<String>> complete(Long id) {
        if (id == null) {
            return Mono.just(Result.error("派单失败"));
        }
        return orderRepository.queryById(id)
                .switchIfEmpty(Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)))
                .flatMap(ordersDB -> {
                    if (!ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
                        return Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR));
                    }
                    Orders orders = Orders.builder()
                            .id(id)
                            .deliveryTime(LocalDateTime.now())
                            .status(Orders.COMPLETED)
                            .build();
                    return orderRepository.update(orders)
                            .then(Mono.just(Result.success()));
                });
    }

    public Mono<Void> checkOutOfRange(String address) {
        Map<String, Object> params = new HashMap<>();
        params.put("output", "json");
        params.put("ak", ak);

        // 获取店铺坐标
        return getCoordinate(shopAddress, params)
                .flatMap(shopLngLat -> {
                    // 获取用户坐标
                    return getCoordinate(address, params)
                            .flatMap(userLngLat -> {
                                // 计算配送距离
                                return calculateDistance(shopLngLat, userLngLat, params);
                            });
                });
    }

    private Mono<String> getCoordinate(String address, Map<String, Object> params) {
        params.put("address", address);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/geocoding/v3")
                        .queryParam("address", address)
                        .queryParam("output", params.get("output"))
                        .queryParam("ak", params.get("ak"))
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    JSONObject jsonObject = JSON.parseObject(response);
                    if (!jsonObject.getString("status").equals("0")) {
                        return Mono.error(new OrderBusinessException("地址解析失败: " + address));
                    }
                    JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
                    String lat = location.getString("lat");
                    String lng = location.getString("lng");
                    return Mono.just(lat + "," + lng);
                });
    }

    private Mono<Void> calculateDistance(String origin, String destination, Map<String, Object> params) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/directionlite/v1/driving")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("steps_info", "0")
                        .queryParam("output", params.get("output"))
                        .queryParam("ak", params.get("ak"))
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    JSONObject jsonObject = JSON.parseObject(response);
                    if (!jsonObject.getString("status").equals("0")) {
                        return Mono.error(new OrderBusinessException("配送路线规划失败"));
                    }
                    JSONObject result = jsonObject.getJSONObject("result");
                    JSONArray routes = result.getJSONArray("routes");
                    int distance = routes.getJSONObject(0).getIntValue("distance");
                    if (distance > 5000) {
                        return Mono.error(new OrderBusinessException("超出配送范围"));
                    }
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> reminder(Long id) {
        return orderRepository.queryById(id)
                .switchIfEmpty(Mono.error(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)))
                .flatMap(orders -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", 2);
                    map.put("orderId", id);
                    map.put("content", "订单号" + id);
                    String json = JSON.toJSONString(map);
                    // 假设 webSocketServer.sendAllClient 方法返回 Mono<Void> 以支持响应式编程
                    return webSocketServer.sendAllClient(json);
                });
    }
}