package com.example.ordermanagement.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "TransportationMethod")
public class ItemTransportation {

    public int getTransportationId() {
		return transportationId;
	}

	public void setTransportationId(int transportationId) {
		this.transportationId = transportationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int transportationId;

    private String name; // 運送方式名稱
}