package com.example.ordermanagement.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@Entity
@Table(name = "ItemOption")
public class ItemOption {
    
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOptionName() {
		return optionName;
	}

	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getOptionPrice() {
		return optionPrice;
	}

	public void setOptionPrice(BigDecimal optionPrice) {
		this.optionPrice = optionPrice;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String optionName;  // 選項名稱

    @Column(nullable = false)
    private int quantity; // 庫存數量
    
    @Column(precision = 10, scale = 2)
    private BigDecimal optionPrice; // 選項額外價格

    // 關聯到商品，設定為懶加載
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId", nullable = false)
    private Item item;
}