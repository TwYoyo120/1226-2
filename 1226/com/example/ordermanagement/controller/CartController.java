package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.CartDto;
import com.example.ordermanagement.dto.CheckoutRequest;
import com.example.ordermanagement.model.*;
import com.example.ordermanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

@Controller
public class CartController {

    private final CartService cartService;
    private final UserInfoService userInfoService;

    public CartController(CartService cartService, UserInfoService userInfoService) {
        this.cartService = cartService;
        this.userInfoService = userInfoService;
    }

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShippingInfoService shippingInfoService;

    @Autowired
    private ShippingMethodService shippingMethodService;

    // 提取 userId 的私有方法
    private Integer getUserIdFromSession(HttpSession session) {
        LoginBean user = (LoginBean) session.getAttribute("user");
        if (user != null) {
            return user.getUserId();
        }
        return null;
    }

    @GetMapping("/cart/item/{itemId}")
    public String viewItemFromCart(@PathVariable Integer itemId, Model model) {
        Item item = itemService.findItemById(itemId);
        if (item == null) {
            return "errorPage"; // 顯示錯誤頁面
        }
        model.addAttribute("item", item);
        model.addAttribute("photos", item.getItemPhoto());
        model.addAttribute("sizeOptions", item.getItemOption());
        return "item/itemView"; // 返回商品詳細頁面
    }

    @GetMapping("/cart")
    public String showCartPage(HttpSession session, Model model) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return "redirect:/login"; // 未登入時重定向到登入頁面
        }
        UserInfo userInfo = userInfoService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID"));
        Cart cart = cartService.getOrCreateCart(userInfo);
        model.addAttribute("cart", cartService.convertToCartDto(cart));
        return "cart/cart"; // 返回購物車頁面
    }

    @GetMapping("/cart/checkout")
    public String showCheckoutPage(HttpSession session, Model model) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return "redirect:/login"; // 未登入時重定向到登入頁面
        }
        // 加載必要數據
        return "cart/checkout"; // 返回 checkout.html
    }

    
    
    @ResponseBody
    @GetMapping("/api/cart")
    public ResponseEntity<?> getCart(HttpSession session) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("尚未登入，請先登入！");
        }

        try {
            UserInfo userInfo = userInfoService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID"));

            Cart cart = cartService.getOrCreateCart(userInfo);
            CartDto cartDto = cartService.convertToCartDto(cart);
            return ResponseEntity.ok(cartDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("無法獲取購物車數據：" + e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/cart/items")
    public ResponseEntity<Map<String, String>> addItemToCart(@RequestBody AddCartItemRequest request, HttpSession session) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return createErrorResponse("尚未登入，請先登入！", HttpStatus.UNAUTHORIZED);
        }

        try {
            UserInfo userInfo = userInfoService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID"));

            cartService.addItemToCart(userInfo, request.getOptionId(), request.getItemQuantity());
            return createSuccessResponse("商品已成功新增至購物車！");
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("加入購物車失敗：" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseBody
    @PutMapping("/api/cart/items/{cartItemId}")
    public ResponseEntity<?> updateCartItemQuantity(
            @PathVariable Integer cartItemId,
            @RequestParam Integer quantity,
            HttpSession session) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("尚未登入，請先登入！");
        }

        try {
            if (quantity <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("數量必須大於 0！");
            }

            UserInfo userInfo = userInfoService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID"));

            CartDto updatedCart = cartService.updateItemQuantity(userInfo, cartItemId, quantity);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("伺服器內部錯誤：" + e.getMessage());
        }
    }

    @ResponseBody
    @DeleteMapping("/api/cart/items/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Integer cartItemId, HttpSession session) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("尚未登入，請先登入！");
        }

        try {
            UserInfo userInfo = userInfoService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID"));

            cartService.removeItemFromCart(userInfo, cartItemId);
            return ResponseEntity.ok("商品已成功移除！");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("移除商品失敗：" + e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/cart/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest checkoutRequest, HttpSession session) {
        Integer userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("尚未登入，請先登入！");
        }

        try {
            Cart cart = cartService.getCartByBuyerId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("購物車不存在"));

            ShippingMethod shippingMethod = shippingMethodService.getMethodById(checkoutRequest.getShippingMethodId())
                    .orElseThrow(() -> new IllegalArgumentException("無效的物流方式"));

            Order order = orderService.createOrderFromCart(cart, checkoutRequest);

            shippingInfoService.saveShippingInfo(order.getShippingInfo());

            cartService.clearCart(cart.getBuyer());

            return ResponseEntity.ok(Map.of("orderId", order.getOrderId(), "redirectUrl", "/payment/" + order.getOrderId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("伺服器內部錯誤: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> createErrorResponse(String message, HttpStatus status) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    public static class AddCartItemRequest {
        private int optionId;
        private int itemQuantity;

        public int getOptionId() {
            return optionId;
        }

        public void setOptionId(int optionId) {
            this.optionId = optionId;
        }

        public int getItemQuantity() {
            return itemQuantity;
        }

        public void setItemQuantity(int itemQuantity) {
            this.itemQuantity = itemQuantity;
        }
    }
}
