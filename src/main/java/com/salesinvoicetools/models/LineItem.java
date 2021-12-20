package com.salesinvoicetools.models;

import com.salesinvoicetools.utils.AppUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class LineItem {
	
	
	@Id
	@GeneratedValue
	public long id;

	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name="OWNER_ID")
	public ShopOrder owner;
	
	@ManyToOne(cascade = {CascadeType.PERSIST})
	public Product product;
	
	/**
	 * Gesamtbetrag dieser Rechnungsposition
	 */
	public long totalPriceGross;

	public long quantity;

	public String variation;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ShopOrder getOwner() {
		return owner;
	}

	public void setOwner(ShopOrder owner) {
		this.owner = owner;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public long getTotalPriceGross() {
		return totalPriceGross;
	}

	public void setTotalPriceGross(long totalPriceGross) {
		this.totalPriceGross = totalPriceGross;
	}

	public long getQuantity() {
		return quantity;
	}

	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}

	public String getVariation() {
		return variation;
	}

	public void setVariation(String variation) {
		this.variation = variation;
	}

	public long getSingleItemAmount() {
		return (long)Math.round(1d * totalPriceGross / quantity);
	}

	public String getSingleItemAmountString() {
		return AppUtils.formatCurrencyAmount(getSingleItemAmount());
	}
	
}
