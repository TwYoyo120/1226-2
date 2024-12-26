package com.example.ordermanagement.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;


@NoArgsConstructor
@Entity
@Table(name = "Item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int itemId;

    public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
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

	public Date getItemDate() {
		return itemDate;
	}

	public void setItemDate(Date itemDate) {
		this.itemDate = itemDate;
	}

	public Date getItemDue() {
		return itemDue;
	}

	public void setItemDue(Date itemDue) {
		this.itemDue = itemDue;
	}

	public String getItemLocation() {
		return itemLocation;
	}

	public void setItemLocation(String itemLocation) {
		this.itemLocation = itemLocation;
	}

	public String getItemInfo() {
		return itemInfo;
	}

	public void setItemInfo(String itemInfo) {
		this.itemInfo = itemInfo;
	}

	public int getItemSell() {
		return itemSell;
	}

	public void setItemSell(int itemSell) {
		this.itemSell = itemSell;
	}

	public boolean isItemDeleteStatus() {
		return itemDeleteStatus;
	}

	public void setItemDeleteStatus(boolean itemDeleteStatus) {
		this.itemDeleteStatus = itemDeleteStatus;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public Brand getBrand() {
		return brand;
	}

	public void setBrand(Brand brand) {
		this.brand = brand;
	}

	public List<ItemPhoto> getItemPhoto() {
		return itemPhoto;
	}

	public void setItemPhoto(List<ItemPhoto> itemPhoto) {
		this.itemPhoto = itemPhoto;
	}

	public List<ItemOption> getItemOption() {
		return itemOption;
	}

	public void setItemOption(List<ItemOption> itemOption) {
		this.itemOption = itemOption;
	}

	public List<ItemOption> getItemOptions() {
		return itemOptions;
	}

	public void setItemOptions(List<ItemOption> itemOptions) {
		this.itemOptions = itemOptions;
	}

	public List<ItemTransportation> getTransportationMethods() {
		return transportationMethods;
	}

	public void setTransportationMethods(List<ItemTransportation> transportationMethods) {
		this.transportationMethods = transportationMethods;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}

	private String itemName;

    private BigDecimal itemPrice;
    
    private Date itemDate;
    
    private Date itemDue;
    
    private String itemLocation;

    private String itemInfo;
    
    private int itemSell = 0;

    private boolean itemDeleteStatus = false;

    // 建立多對一關聯：每個 Item 都對應一個 Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemCategoryId", referencedColumnName = "categoryId")
    private Category category;

    //品牌
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemBrandId", referencedColumnName = "brandId")
    private Brand brand;

    // 商品圖片
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "item")
    @JsonIgnore
    private List<ItemPhoto> itemPhoto = new ArrayList<>();

    
    // 產品選項和庫存數量
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "item", orphanRemoval = true)
    private List<ItemOption> itemOption = new ArrayList<>();
    
    //Mantle
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemOption> itemOptions;
    
    // 多對多關聯到運送方式
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "Item_Transportation", // 中間表名稱
        joinColumns = @JoinColumn(name = "item_id"), // 商品的外鍵
        inverseJoinColumns = @JoinColumn(name = "transportation_id") // 運送方式的外鍵
    )
    private List<ItemTransportation> transportationMethods = new ArrayList<>();
    
    //賣家ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId",nullable = false)
    private UserInfo userInfo;

    // 在插入之前自動設定 itemDate
    @PrePersist
    public void prePersist() {
        if (itemDate == null) {
            itemDate = new Date(System.currentTimeMillis()); // 設定為當前時間
        }
    }
    
    
}
