package com.example.ordermanagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ordermanagement.dto.CheckoutRequest;
import com.example.ordermanagement.dto.ShippingInfoRequest;
import com.example.ordermanagement.model.ShippingInfo;
import com.example.ordermanagement.model.UserInfo;
import com.example.ordermanagement.service.ShippingInfoService;
import com.example.ordermanagement.service.UserInfoService;
import com.example.ordermanagement.model.ShippingMethod;
import com.example.ordermanagement.service.ShippingMethodService;


import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private ShippingInfoService shippingInfoService;

    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private ShippingMethodService shippingMethodService;


    @PostMapping("/shipping")
    public ResponseEntity<String> saveShippingInfo(@RequestBody CheckoutRequest checkoutRequest, HttpSession session) {
//        Integer userId = (Integer) session.getAttribute("userId");
//        if (userId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
//        }
    	Integer userId = 1; // 測試用的固定用戶 ID（用於排除 Session 問題）
        UserInfo currentUser = userInfoService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("無效的用戶ID：" + userId));

        // 使用 checkoutRequest 和 UserInfo 保存物流信息
        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setShippingInfoRecipient(checkoutRequest.getShippingInfoRecipient());
        shippingInfo.setShippingInfoAddress(checkoutRequest.getShippingInfoAddress());
        shippingInfo.setShippingInfoStatus("Pending");

        ShippingMethod shippingMethod = shippingMethodService.getMethodById(checkoutRequest.getShippingMethodId())
                .orElseThrow(() -> new IllegalArgumentException("無效的物流方式"));
        shippingInfo.setShippingMethod(shippingMethod);

        shippingInfoService.saveShippingInfo(shippingInfo);
        return ResponseEntity.ok("物流資訊已成功儲存");
    }

}
