package com.example.stocksync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "vendor"}))
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Integer stockQuantity;

	@Column(nullable = false)
	private String vendor;

	public Product() {
	}

	public Product(String sku, String name, Integer stockQuantity, String vendor) {
		this.sku = sku;
		this.name = name;
		this.stockQuantity = stockQuantity;
		this.vendor = vendor;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(Integer stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
}
