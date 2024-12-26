package com.example.ordermanagement.controller;

import com.example.ordermanagement.model.Item;
import com.example.ordermanagement.model.LoginBean;
import com.example.ordermanagement.model.Order;
import com.example.ordermanagement.model.OrderItem;
import com.example.ordermanagement.model.OrderRepository;
import com.example.ordermanagement.model.UserInfo;
import com.example.ordermanagement.service.OrderService;
import com.example.ordermanagement.service.UserInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;  // ★ 新增，使用 HttpSession
import org.springframework.ui.Model;      // ★ 確保你有這個 import

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 顯示訂單管理頁面（管理員）
     */
    @GetMapping("/order-management")
    public String adminOrderManagement() {
        return "order/adminOrderManagement";
    }

    /**
     * 獲取訂單列表（支持篩選條件和分頁）
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String shippingStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 狀態映射
        String mappedOrderStatus = mapOrderStatus(orderStatus);
        String mappedPaymentStatus = mapPaymentStatus(paymentStatus);
        String mappedShippingStatus = mapShippingStatus(shippingStatus);

        var result = orderService.getOrdersByFilters(
                buyerId, sellerId, orderId, mappedOrderStatus, mappedPaymentStatus, mappedShippingStatus, startDate, endDate, PageRequest.of(page - 1, size));

        var orders = result.getContent().stream().map(order -> Map.of(
                "orderId", order.getOrderId(),
                "buyerId", order.getBuyer() != null ? order.getBuyer().getUserId() : null,
                "sellerId", order.getOrderItems() != null && !order.getOrderItems().isEmpty()
                        ? order.getOrderItems().get(0).getSeller().getUserId()
                        : null,
                "orderTotal", order.getOrderTotal(),
                "paymentStatus", order.getPaymentStatus(),
                "shippingStatus", order.getShippingStatus(),
                "orderStatus", order.getOrderStatus()
        )).toList();

        return ResponseEntity.ok(Map.of(
                "content", orders,
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber() + 1
        ));
    }

    private String mapOrderStatus(String orderStatus) {
        if (orderStatus == null) return null;
        return switch (orderStatus) {
            case "待處理" -> "Pending";
            case "處理中" -> "Processing";
            case "已完成" -> "Completed";
            case "取消" -> "Cancelled";
            case "退貨/退款" -> "Refunded";
            default -> orderStatus;
        };
    }

    private String mapPaymentStatus(String paymentStatus) {
        if (paymentStatus == null) return null;
        return switch (paymentStatus) {
            case "待付款" -> "Unpaid";
            case "已付款" -> "Paid";
            default -> paymentStatus;
        };
    }

    private String mapShippingStatus(String shippingStatus) {
        if (shippingStatus == null) return null;
        return switch (shippingStatus) {
            case "未出貨" -> "Not Shipped";
            case "運送中" -> "Shipped";
            case "待收貨" -> "Pending Receipt";
            case "已收貨" -> "Delivered";
            default -> shippingStatus;
        };
    }

    /**
     * 根據訂單ID獲取訂單詳情
     */
    @GetMapping("/{orderId}")
    @ResponseBody
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        System.out.println("Fetching order details for orderId: " + orderId);

        Optional<Order> order = orderRepository.findByIdWithItems(orderId);

        if (order.isPresent()) {
            Order foundOrder = order.get();
            System.out.println("Order found:");
            System.out.println("Order ID: " + foundOrder.getOrderId());
            System.out.println("Order Total: " + foundOrder.getOrderTotal());
            System.out.println("Order Status: " + foundOrder.getOrderStatus());

            // 構建返回的 JSON 結構
            Map<String, Object> response = Map.of(
                "orderId", foundOrder.getOrderId(),
                "orderDate", foundOrder.getOrderDate(),
                "orderTotal", foundOrder.getOrderTotal(),
                "orderStatus", foundOrder.getOrderStatus(),
                "paymentStatus", foundOrder.getPaymentStatus(),
                "shippingStatus", foundOrder.getShippingStatus(),
                "buyer", Map.of(
                    "buyerId", foundOrder.getBuyer() != null ? foundOrder.getBuyer().getUserId() : null,
                    "buyerName", foundOrder.getBuyer() != null ? foundOrder.getBuyer().getUserName() : "未知"
                ),
                "orderItems", foundOrder.getOrderItems().stream().map(item -> Map.of(
                    "sellerId", item.getSeller() != null ? item.getSeller().getUserId() : null,
                    "itemId", item.getItem() != null ? item.getItem().getItemId() : null,
                    "itemName", item.getItem() != null ? item.getItem().getItemName() : "未知",
                    "itemQuantity", item.getItemQuantity(),
                    "itemPrice", item.getItemPrice(),
                    "itemSize", item.getItemSize() != null ? item.getItemSize().getOptionName() : "無"
                )).toList()
            );

            return ResponseEntity.ok(response);
        } else {
            System.out.println("No order found with orderId: " + orderId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新訂單詳情（支持付款狀態、物流狀態和訂單狀態）
     */
    @PutMapping("/{orderId}")
    @ResponseBody
    public ResponseEntity<?> updateOrderDetails(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> updatedFields) {
        try {
            boolean updated = orderService.updateOrderDetails(
                    orderId,
                    (String) updatedFields.get("paymentStatus"),
                    (String) updatedFields.get("shippingStatus"),
                    (String) updatedFields.get("orderStatus")
            );
            if (updated) {
                // 返回有效的 JSON 響應
                return ResponseEntity.ok(Map.of("message", "Order updated successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 建立新訂單
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Order> createOrder(@RequestParam Integer buyerId, @RequestBody Order order) {
        UserInfo buyer = userInfoService.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("無效的買家ID"));
        order.setBuyer(buyer);

        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getItem() == null || orderItem.getItem().getUserInfo() == null) {
                throw new IllegalArgumentException("商品信息或賣家信息無效");
            }
            orderItem.setSeller(orderItem.getItem().getUserInfo());
            orderItem.setOrder(order);
        }

        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.ok(createdOrder);
    }

    /**
     * 棄單功能
     */
    @PostMapping("/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            boolean canceled = orderService.cancelOrder(orderId);
            return canceled ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // 以下為「買家/賣家」各自訂單頁面 (改用 SessionAttribute 拿 userId)
    // -----------------------------------------------------------------

    /**
     * 顯示購買清單頁面
     */
    @GetMapping("/buyer-orders")
    public String getBuyerOrdersPage(HttpSession session, Model model) {
        LoginBean user = (LoginBean) session.getAttribute("user");
        if (user == null) {
            // 用戶未登入，重定向到登入頁面
            return "redirect:/login";
        }

        // 打印用戶資訊以供調試
        System.out.println("User is logged in: " + user.getUserEmail());

        // 將 int 轉換為 Long
        Long buyerId = Long.valueOf(user.getUserId());

        // 獲取用戶的購買訂單，使用轉換後的 Long 類型
        model.addAttribute("buyerOrders", orderService.getOrdersByBuyerId(buyerId));
        return "order/buyerOrder"; // 返回對應的 Thymeleaf 模板
    }

    /**
     * 獲取購買清單數據（支持篩選）
     */
    @GetMapping("/buyer-orders/data")
    @ResponseBody
    public ResponseEntity<?> getBuyerOrdersData(@SessionAttribute("userId") Long userId) {

        var orders = orderService.getOrdersByBuyerId(userId);
        var response = orders.stream().map(order -> Map.of(
            "orderId", order.getOrderId(),
            "sellerName", order.getOrderItems().isEmpty() ? "未知" : order.getOrderItems().get(0).getSeller().getUserName(),
            "orderTotal", order.getOrderTotal(),
            "orderStatus", order.getOrderStatus(),
            "orderItems", order.getOrderItems().stream().map(orderItem -> {
                Item item = orderItem.getItem();
                String base64Photo = null;
                if (item != null && item.getItemPhoto() != null && !item.getItemPhoto().isEmpty()) {
                    base64Photo = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(item.getItemPhoto().get(0).getPhotoFile());
                }
                return Map.of(
                    "itemName", item != null ? item.getItemName() : "未知",
                    "itemPhotoUrl", base64Photo,
                    "itemPrice", item != null ? item.getItemPrice() : null,
                    "itemQuantity", orderItem.getItemQuantity()
                );
            }).toList()
        )).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 處理付款請求
     */
    @PostMapping("/buyer/{orderId}/pay")
    @ResponseBody
    public ResponseEntity<?> processPayment(@PathVariable Long orderId, @SessionAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(403).body("用戶未登入");
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("訂單不存在");
        }

        Order order = orderOpt.get();
        if (!order.getBuyer().getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("無權訪問該訂單");
        }

        // 這裡加入付款邏輯，例如更新付款狀態
        boolean paymentSuccess = orderService.updateOrderDetails(orderId, "Paid", null, null);
        if (paymentSuccess) {
            return ResponseEntity.ok(Map.of("message", "付款成功"));
        } else {
            return ResponseEntity.status(500).body(Map.of("error", "付款失敗"));
        }
    }

    /**
     * 顯示賣家訂單頁面
     */

    @GetMapping("/seller-orders")
    public String getSellerOrdersPage(HttpSession session, Model model) {
        LoginBean user = (LoginBean) session.getAttribute("user");
        if (user == null) {
            // 用戶未登入，重定向到登入頁面
            return "redirect:/login";
        }

        // 打印用戶資訊以供調試
        System.out.println("User is logged in: " + user.getUserEmail());

        // 將 int 轉換為 Long
        Long sellerId = Long.valueOf(user.getUserId());

        // 獲取用戶的購買訂單，使用轉換後的 Long 類型
        model.addAttribute("sellerOrders", orderService.getOrdersBySellerId(sellerId));
        return "order/sellerOrder"; // 返回對應的 Thymeleaf 模板
    }


    /**
     * 獲取賣家訂單數據（支持篩選）
     */
    @GetMapping("/seller-orders/data")
    @ResponseBody
    public ResponseEntity<?> getSellerOrdersData(@SessionAttribute("userId") Long userId) {

        var orders = orderService.getOrdersBySellerId(userId);
        var response = orders.stream().map(order -> Map.of(
            "orderId", order.getOrderId(),
            "buyerName", order.getBuyer() != null ? order.getBuyer().getUserName() : "未知",
            "orderTotal", order.getOrderTotal(),
            "orderStatus", order.getOrderStatus(),
            "paymentStatus", order.getPaymentStatus(),
            "shippingStatus", order.getShippingStatus(),
            "orderItems", order.getOrderItems().stream().map(orderItem -> {
                Item item = orderItem.getItem();
                String base64Photo = null;
                if (item != null && item.getItemPhoto() != null && !item.getItemPhoto().isEmpty()) {
                    base64Photo = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(item.getItemPhoto().get(0).getPhotoFile());
                }
                return Map.of(
                    "itemName", item != null ? item.getItemName() : "未知",
                    "itemPhotoUrl", base64Photo,
                    "itemPrice", item != null ? item.getItemPrice() : null,
                    "itemQuantity", orderItem.getItemQuantity()
                );
            }).toList()
        )).toList();

        return ResponseEntity.ok(response);
    }


    /**
     * 買家端 - 付款 (更新付款狀態)
     * POST /orders/buyer/{orderId}/pay
     */
    @GetMapping("/buyer/{orderId}/pay")
    public String redirectToPaymentPage(@PathVariable Long orderId, Model model, @SessionAttribute("userId") Long userId) {
        var order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("訂單不存在"));

        if (!order.getBuyer().getUserId().equals(userId)) {
            throw new IllegalStateException("無權訪問該訂單");
        }

        model.addAttribute("order", order);

        // 導向金流頁面
        return "payment/paymentPage";
    }

    /**
     * 賣家端 - 出貨 (更新物流狀態)
     * POST /orders/seller/{orderId}/ship
     */
    @PostMapping("/seller/{orderId}/ship")
    @ResponseBody
    public ResponseEntity<?> shipOrder(@PathVariable Long orderId, @SessionAttribute("userId") Long userId) {
        // 獲取訂單
        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(404).body("訂單不存在");
        }

        Order order = orderOpt.get();

        // 檢查當前用戶是否為訂單的賣家
        boolean isSeller = order.getOrderItems().stream()
                .anyMatch(orderItem -> orderItem.getSeller().getUserId().equals(userId));

        if (!isSeller) {
            return ResponseEntity.status(403).body("無權訪問該訂單");
        }

        // 更新物流狀態為 "Shipped"
        boolean updated = orderService.updateOrderDetails(orderId, null, "Shipped", null);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "出貨成功"));
        } else {
            return ResponseEntity.status(500).body(Map.of("error", "出貨失敗"));
        }
    }

}
