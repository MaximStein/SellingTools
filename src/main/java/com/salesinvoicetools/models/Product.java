package com.salesinvoicetools.models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.salesinvoicetools.shopapis.ShopApiBase.*;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "marketplace", "productNumber" }) })
public class Product {

	@OneToMany(mappedBy = "product", cascade = { CascadeType.MERGE })
	public List<LineItem> lineItems = new ArrayList<>();

	@Id
	@GeneratedValue
	public long id;

	public long grossPrice;

	public String description;

	public String productNumber;

	@Enumerated(EnumType.STRING)
	public Marketplace marketplace;

	public Product() {
	}

	public Product(Marketplace m, String productNumber) {
		this.productNumber = productNumber;
		this.marketplace = m;
	}

	public Product(long grossPrice, String description, String productNumber, Marketplace mp) {
		this(mp, productNumber);
		this.grossPrice = grossPrice;
		this.description = description;
	}

	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItem> lineItems) {
		this.lineItems = lineItems;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getGrossPrice() {
		return grossPrice;
	}

	public void setGrossPrice(long grossPrice) {
		this.grossPrice = grossPrice;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}

	public Marketplace getMarketplace() {
		return marketplace;
	}

	public void setMarketplace(Marketplace marketplace) {
		this.marketplace = marketplace;
	}
}
