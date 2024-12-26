package com.example.ordermanagement.service;

import com.example.ordermanagement.model.Order;
import com.example.ordermanagement.model.OrderRepository;
import com.example.ordermanagement.model.ShippingInfo;
import com.example.ordermanagement.model.ShippingInfoRepository;
import com.example.ordermanagement.model.ShippingMethod;
import com.example.ordermanagement.model.ShippingMethodRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ShippingInfoService {

    @Autowired
    private ShippingInfoRepository shippingInfoRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShippingMethodRepository shippingMethodRepository;

    /**
     * 更新物流信息
     */
    public boolean updateShippingInfo(Long shippingInfoId, ShippingInfo updatedInfo, MultipartFile imageFile) {
        ShippingInfo existingInfo = shippingInfoRepository.findById(shippingInfoId)
            .orElseThrow(() -> new RuntimeException("找不到指定的物流信息"));

        // 確保唯一
        validateUniqueShippingInfoForOrder(existingInfo.getOrder().getOrderId(), shippingInfoId);

        // 更新基本資訊
        updateBasicShippingInfo(existingInfo, updatedInfo);

        // 若有物流方式ID就去找
        if (updatedInfo.getShippingMethod() != null
            && updatedInfo.getShippingMethod().getShippingMethodId() != null) {
            ShippingMethod method = findShippingMethodById(
                updatedInfo.getShippingMethod().getShippingMethodId()
            );
            existingInfo.setShippingMethod(method);
        }

        // 若有上傳圖片
        if (imageFile != null && !imageFile.isEmpty()) {
            existingInfo.setShippingInfoImage(uploadImage(imageFile));
        }

        shippingInfoRepository.save(existingInfo);
        return true;
    }

    private void validateUniqueShippingInfoForOrder(Long orderId, Long shippingInfoId) {
        Optional<ShippingInfo> existingShippingInfo = shippingInfoRepository.findByOrderOrderId(orderId);
        if (existingShippingInfo.isPresent()
            && !existingShippingInfo.get().getShippingInfoId().equals(shippingInfoId)) {
            throw new IllegalArgumentException("該訂單已存在物流信息，無法重複設置。");
        }
    }

    private void updateBasicShippingInfo(ShippingInfo existingInfo, ShippingInfo updatedInfo) {
        existingInfo.setShippingInfoRecipient(updatedInfo.getShippingInfoRecipient());
        existingInfo.setShippingInfoAddress(updatedInfo.getShippingInfoAddress());
        existingInfo.setShippingInfoStatus(updatedInfo.getShippingInfoStatus());
        existingInfo.setShippingInfoTrackingNumber(updatedInfo.getShippingInfoTrackingNumber());
    }

    private byte[] uploadImage(MultipartFile imageFile) {
        if (imageFile.getContentType() == null 
            || !imageFile.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("只支持圖片檔案格式");
        }
        try {
            return imageFile.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("圖片上傳失敗: " + e.getMessage(), e);
        }
    }

    private ShippingMethod findShippingMethodById(Long methodId) {
        return shippingMethodRepository.findById(methodId)
            .orElseThrow(() -> new RuntimeException("指定的物流方式不存在"));
    }

    /**
     * 新增並儲存一筆物流資訊（前提：必須已關聯訂單）
     */
    public ShippingInfo saveShippingInfo(ShippingInfo shippingInfo) {
        validateOrderAssociation(shippingInfo.getOrder());
        return shippingInfoRepository.save(shippingInfo);
    }

    private void validateOrderAssociation(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("物流信息必須關聯到一個訂單");
        }
    }

    // ---------------------------------------------------------------
    // 查詢功能
    // ---------------------------------------------------------------

    public List<ShippingInfo> getShippingInfoByAllTrackingNumber(String trackingNumber) {
        return shippingInfoRepository.findByShippingInfoTrackingNumber(trackingNumber);
    }

    public Optional<ShippingInfo> getShippingInfoByTrackingNumber(String trackingNumber) {
        List<ShippingInfo> resultList = shippingInfoRepository.findByShippingInfoTrackingNumber(trackingNumber);
        return resultList.stream().findFirst();
    }

    public Optional<ShippingInfo> getShippingInfoById(Long shippingInfoId) {
        return shippingInfoRepository.findById(shippingInfoId);
    }

    /**
     * 查詢訂單與物流信息的關聯數據
     * 
     * @param orderId          訂單ID
     * @param sellerId         賣家ID
     * @param buyerId          買家ID
     * @param recipient        收件人
     * @param shippingInfoStatus 物流狀態
     * @param trackingNumber   物流追蹤號
     */
    public List<Map<String, Object>> getOrderShippingInfo(
        Long orderId, 
        Long sellerId,
        Long buyerId,
        String recipient,
        String shippingInfoStatus,
        String trackingNumber

    ) {
        // 注意順序與 createShippingInfoSpecification(...) 對齊
        Specification<ShippingInfo> spec = createShippingInfoSpecification(
            orderId, shippingInfoStatus, trackingNumber, sellerId, buyerId, recipient
        );

        List<ShippingInfo> shippingInfos = shippingInfoRepository.findAll(spec);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ShippingInfo info : shippingInfos) {
            // 組成回傳 map
            Map<String, Object> combined = new HashMap<>();

            // shippingInfoMap
            Map<String, Object> shippingInfoMap = new HashMap<>();
            shippingInfoMap.put("shippingInfoId", info.getShippingInfoId());
            shippingInfoMap.put("shippingInfoRecipient", info.getShippingInfoRecipient());
            shippingInfoMap.put("shippingInfoAddress", info.getShippingInfoAddress());
            shippingInfoMap.put("shippingInfoStatus", info.getShippingInfoStatus());
            shippingInfoMap.put("shippingInfoTrackingNumber", info.getShippingInfoTrackingNumber());
            if (info.getShippingInfoImage() != null) {
                String base64Image = Base64.getEncoder().encodeToString(info.getShippingInfoImage());
                shippingInfoMap.put("shippingInfoImage", base64Image);
            }

            // orderMap
            Map<String, Object> orderMap = new HashMap<>();
            if (info.getOrder() != null) {
                orderMap.put("orderId", info.getOrder().getOrderId());
                // 買家
                if (info.getOrder().getBuyer() != null) {
                    orderMap.put("buyerId", info.getOrder().getBuyer().getUserId());
                }
                // 賣家
                if (info.getOrder().getOrderItems() != null && !info.getOrder().getOrderItems().isEmpty()) {
                    orderMap.put(
                        "sellerId",
                        info.getOrder().getOrderItems().get(0).getSeller().getUserId()
                    );
                }
            }

            // shippingMethodMap
            Map<String, Object> shippingMethodMap = new HashMap<>();
            if (info.getShippingMethod() != null) {
                shippingMethodMap.put("shippingMethodId", info.getShippingMethod().getShippingMethodId());
                shippingMethodMap.put("methodName", info.getShippingMethod().getMethodName());
            }

            combined.put("shippingInfo", shippingInfoMap);
            combined.put("order", orderMap);
            combined.put("shippingMethod", shippingMethodMap);

            result.add(combined);
        }
        return result;
    }

    /**
     * 取得單筆物流信息（回傳 JSON Map）
     */
    public Map<String, Object> getShippingInfoMapById(Long shippingInfoId) {
        ShippingInfo info = shippingInfoRepository.findById(shippingInfoId)
          .orElseThrow(() -> new RuntimeException("找不到指定的物流信息 ID=" + shippingInfoId));
        return toShippingInfoMap(info);
    }

    private Map<String, Object> toShippingInfoMap(ShippingInfo info) {
        Map<String, Object> shippingInfoMap = new HashMap<>();
        shippingInfoMap.put("shippingInfoId", info.getShippingInfoId());
        shippingInfoMap.put("shippingInfoRecipient", info.getShippingInfoRecipient());
        shippingInfoMap.put("shippingInfoAddress", info.getShippingInfoAddress());
        shippingInfoMap.put("shippingInfoStatus", info.getShippingInfoStatus());
        shippingInfoMap.put("shippingInfoTrackingNumber", info.getShippingInfoTrackingNumber());

        if (info.getShippingInfoImage() != null) {
            String base64Image = Base64.getEncoder().encodeToString(info.getShippingInfoImage());
            shippingInfoMap.put("shippingInfoImage", base64Image);
        }

        Map<String, Object> orderMap = new HashMap<>();
        if (info.getOrder() != null) {
            orderMap.put("orderId", info.getOrder().getOrderId());
            if (info.getOrder().getBuyer() != null) {
                orderMap.put("buyerId", info.getOrder().getBuyer().getUserId());
            }
            if (info.getOrder().getOrderItems() != null && !info.getOrder().getOrderItems().isEmpty()) {
                orderMap.put("sellerId", 
                    info.getOrder().getOrderItems().get(0).getSeller().getUserId()
                );
            }
        }

        Map<String, Object> shippingMethodMap = new HashMap<>();
        if (info.getShippingMethod() != null) {
            shippingMethodMap.put("shippingMethodId", info.getShippingMethod().getShippingMethodId());
            shippingMethodMap.put("methodName", info.getShippingMethod().getMethodName());
        }

        Map<String, Object> combined = new HashMap<>();
        combined.put("shippingInfo", shippingInfoMap);
        combined.put("order", orderMap);
        combined.put("shippingMethod", shippingMethodMap);

        return combined;
    }

    // ----------------------------------------------------------
    // 動態條件：同時支援 orderId, shippingInfoStatus, trackingNumber, sellerId, buyerId, recipient
    // ----------------------------------------------------------
    private Specification<ShippingInfo> createShippingInfoSpecification(
        Long orderId,
        String shippingInfoStatus,
        String trackingNumber,
        Long sellerId,
        Long buyerId,
        String recipient
    ) {
        Specification<ShippingInfo> spec = Specification.where(null);

        if (orderId != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.join("order").get("orderId"), orderId)
            );
        }
        if (shippingInfoStatus != null && !shippingInfoStatus.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("shippingInfoStatus"), shippingInfoStatus)
            );
        }
        if (trackingNumber != null && !trackingNumber.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("shippingInfoTrackingNumber"), trackingNumber)
            );
        }
        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> {
                var orderJoin = root.join("order");
                var itemsJoin = orderJoin.join("orderItems");
                return cb.equal(itemsJoin.join("seller").get("userId"), sellerId);
            });
        }
        if (buyerId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.join("order").join("buyer").get("userId"), buyerId)
            );
        }
        if (recipient != null && !recipient.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.like(root.get("shippingInfoRecipient"), "%" + recipient + "%")
            );
        }

        return spec;
    }

}
