package com.example.ordermanagement.dto;

import java.math.BigDecimal;

public class CartItemDto {
    private Long cartItemId; // 修改為 Long
    private String itemName;
    private BigDecimal itemPrice;
    private int itemQuantity;
    private String imageUrl;
    private String itemSize; // 新增尺寸字段
    // 新增尺寸相關字段
    private String itemSizeName; // 尺寸名稱
    private BigDecimal itemSizePrice; // 尺寸額外價格

    // Getters and Setters
    public Long getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public int getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getItemSizeName() {
        return itemSizeName;
    }

    public void setItemSizeName(String itemSizeName) {
        this.itemSizeName = itemSizeName;
    }

    public BigDecimal getItemSizePrice() {
        return itemSizePrice;
    }

    public void setItemSizePrice(BigDecimal itemSizePrice) {
        this.itemSizePrice = itemSizePrice;
    }

	public String getItemSize() {
		return itemSize;
	}

	public void setItemSize(String itemSize) {
		this.itemSize = itemSize;
	}
}
