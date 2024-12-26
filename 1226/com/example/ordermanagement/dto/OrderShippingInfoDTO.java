package com.example.ordermanagement.dto;

import com.example.ordermanagement.model.Order;
import com.example.ordermanagement.model.ShippingInfo;

public class OrderShippingInfoDTO {
    private Order order;
    private ShippingInfo shippingInfo;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ShippingInfo getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(ShippingInfo shippingInfo) {
        this.shippingInfo = shippingInfo;
    }
}
