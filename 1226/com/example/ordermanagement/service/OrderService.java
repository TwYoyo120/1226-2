package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.CheckoutRequest;
import com.example.ordermanagement.model.*;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShippingMethodService shippingMethodService;

    @Autowired
    private ShippingInfoService shippingInfoService;

    @Autowired
    private ShippingInfoRepository shippingInfoRepository;

    /**
     * 創建新訂單
     */
    public Order createOrder(Order order) {
        // 基本欄位處理
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        if (order.getOrderStatus() == null || order.getOrderStatus().isEmpty()) {
            order.setOrderStatus("Pending");
        }
        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(order);
        }

        // 保存訂單
        return orderRepository.save(order);
    }

    /**
     * 根據篩選條件查詢訂單
     */
    public Page<Order> getOrdersByFilters(
            Long buyerId, Long sellerId, Long orderId, String orderStatus,
            String paymentStatus, String shippingStatus,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Specification<Order> spec = Specification.where(null);

        if (orderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderId"), orderId));
        }
        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("orderDate"), startDateTime));
        }
        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("orderDate"), endDateTime));
        }
        if (buyerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("buyer").get("userId"), buyerId));
        }
        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> orderItems = root.join("orderItems", JoinType.INNER);
                return cb.equal(orderItems.get("seller").get("userId"), sellerId);
            });
        }
        if (orderStatus != null && !orderStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderStatus"), orderStatus));
        }
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), paymentStatus));
        }
        if (shippingStatus != null && !shippingStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("shippingStatus"), shippingStatus));
        }

        return orderRepository.findAll(spec, pageable);
    }

    /**
     * 根據ID獲取訂單
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * 更新訂單詳情
     */
    public boolean updateOrderDetails(Long orderId, String paymentStatus, String shippingStatus, String newOrderStatus) {
        return orderRepository.findById(orderId).map(order -> {
            if (paymentStatus != null) order.setPaymentStatus(paymentStatus);
            if (shippingStatus != null) order.setShippingStatus(shippingStatus);
            if (newOrderStatus != null) order.setOrderStatus(newOrderStatus);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    /**
     * 棄單功能
     */
    public boolean cancelOrder(Long orderId) {
        return getOrderById(orderId).map(order -> {
            if ("Canceled".equals(order.getOrderStatus())) {
                throw new IllegalStateException("訂單已被標記為棄單");
            }
            order.setOrderStatus("Canceled");
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    /**
     * 從購物車創建訂單
     */
    @Transactional
    public Order createOrderFromCart(Cart cart, CheckoutRequest checkoutRequest) {
        Order order = new Order();
        order.setBuyer(cart.getBuyer());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("Pending");
        order.setPaymentStatus("Unpaid");
        order.setShippingStatus("Not Shipped");

        BigDecimal totalAmount = cart.getCartItems().stream()
                .map(item -> item.getItemPrice().multiply(BigDecimal.valueOf(item.getItemQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setOrderTotal(totalAmount);

        cart.getCartItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setItemSize(cartItem.getItemSize());
            orderItem.setItemPrice(cartItem.getItemPrice());
            orderItem.setItemQuantity(cartItem.getItemQuantity());
            orderItem.setSeller(cartItem.getSeller());
            order.addOrderItem(orderItem);
        });

        Order savedOrder = orderRepository.save(order);

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setShippingInfoRecipient(checkoutRequest.getShippingInfoRecipient());
        shippingInfo.setShippingInfoAddress(checkoutRequest.getShippingInfoAddress());
        shippingInfo.setShippingInfoStatus("Pending");

        ShippingMethod shippingMethod = shippingMethodService
                .getMethodById(checkoutRequest.getShippingMethodId())
                .orElseThrow(() -> new IllegalArgumentException("無效的物流方式"));
        shippingInfo.setShippingMethod(shippingMethod);
        shippingInfo.setOrder(savedOrder);

        shippingInfoService.saveShippingInfo(shippingInfo);

        return savedOrder;
    }

    /**
     * 根據買家ID獲取訂單
     */
    public List<Order> getOrdersByBuyerId(Long buyerId) {
        return orderRepository.findByBuyer_UserId(buyerId);
    }

    /**
     * 根據賣家ID獲取訂單
     */
    public List<Order> getOrdersBySellerId(Long sellerId) {
        return orderRepository.findAllBySellerUserId(sellerId);
    }

    /**
     * 更新訂單物流狀態
     */
    @Transactional
    public boolean updateOrderShippingStatus(Long orderId, String newStatus) {
        return getOrderById(orderId).map(order -> {
            order.setShippingStatus(newStatus);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }
}
