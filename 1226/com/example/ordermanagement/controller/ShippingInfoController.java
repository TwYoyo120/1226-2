package com.example.ordermanagement.controller;

import com.example.ordermanagement.model.ShippingInfo;
import com.example.ordermanagement.model.ShippingInfoRepository;
import com.example.ordermanagement.service.ShippingInfoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Controller
@RequestMapping("/shipping")
public class ShippingInfoController {

    @Autowired
    private ShippingInfoService shippingInfoService;

    @Autowired
    private ShippingInfoRepository shippingInfoRepository;

    /**
     * 返回物流追蹤頁面
     */
    @GetMapping("/shipping-tracking")
    public String adminShippingTracking() {
        return "shipment/adminShippingTracking";
    }

    /**
     * 查詢物流信息
     *
     * @param orderId        訂單 ID (可選)
     * @param sellerId       賣家ID (可選)
     * @param buyerId        買家ID (可選)
     * @param recipient      收件人 (可選)
     * @param status         物流狀態 (可選)
     * @param trackingNumber 物流追蹤號 (可選)

     * @return 包含訂單和物流信息的結果列表
     */
    @ResponseBody
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getOrderShippingInfo(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) Long sellerId,
        @RequestParam(required = false) Long buyerId,
        @RequestParam(required = false) String recipient,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String trackingNumber
    
    ) {
        // 注意呼叫 ShippingInfoService 時，參數順序要與 Service 的方法一致
        List<Map<String, Object>> results = shippingInfoService.getOrderShippingInfo(
            orderId,
            sellerId,
            buyerId,
            recipient,
            status,
            trackingNumber

        );
        return ResponseEntity.ok(results);
    }

    /**
     * 根據 ID 查詢物流信息
     */
    @ResponseBody
    @GetMapping("/{id}")
    public ResponseEntity<?> getShippingInfoById(@PathVariable Long id) {
        try {
            Map<String, Object> resultMap = shippingInfoService.getShippingInfoMapById(id);
            return ResponseEntity.ok(resultMap);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 查詢物流信息（根據追蹤號）
     * - 返回單一結果（取第一個匹配的元素）
     */
    @ResponseBody
    @GetMapping("/tracking-number")
    public ResponseEntity<?> getShippingInfoByTrackingNumber(@RequestParam String trackingNumber) {
        List<ShippingInfo> shippingInfos = shippingInfoRepository.findByShippingInfoTrackingNumber(trackingNumber);
        Optional<ShippingInfo> shippingInfoOptional = shippingInfos.stream().findFirst();
        return shippingInfoOptional.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 查詢物流信息（根據追蹤號）
     * - 返回所有匹配的結果
     */
    @ResponseBody
    @GetMapping("/tracking-number/all")
    public ResponseEntity<List<ShippingInfo>> getAllShippingInfoByTrackingNumber(@RequestParam String trackingNumber) {
        List<ShippingInfo> shippingInfos = shippingInfoRepository.findByShippingInfoTrackingNumber(trackingNumber);
        if (shippingInfos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(shippingInfos);
    }

    /**
     * 更新物流信息，支持圖片上傳
     */
    @ResponseBody
    @PutMapping("/{id}")
    public ResponseEntity<?> updateShippingInfo(
        @PathVariable Long id,
        @RequestPart("shippingInfo") ShippingInfo updatedInfo,
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            boolean updated = shippingInfoService.updateShippingInfo(id, updatedInfo, imageFile);
            return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失敗: " + e.getMessage());
        }
    }

    /**
     * 取得物流單號圖片 (Base64 格式)
     */
    @GetMapping(value = "/image/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> getImage(@PathVariable Long id) {
        return shippingInfoService.getShippingInfoById(id)
                .filter(info -> info.getShippingInfoImage() != null)
                .map(info -> {
                    String base64Image = Base64.getEncoder().encodeToString(info.getShippingInfoImage());
                    Map<String, String> response = new HashMap<>();
                    response.put("image", "data:image/png;base64," + base64Image);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
