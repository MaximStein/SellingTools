package com.salesinvoicetools.models;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class DataSource {

	@Id
	@GeneratedValue
	public long id;

	@OneToMany(mappedBy = "dataSource")
	public List<ShopOrder> orders;

	@ManyToOne(cascade = CascadeType.PERSIST)
	public OAuth2Token token;

	public Timestamp time;

	public int newEntries;

	public DataSource() {
		this.time = Timestamp.from(Instant.now());
	}

	public DataSource(OAuth2Token token) {
		this();
		this.token = token;
	}
	public DataSource(OAuth2Token token, int newEntries) {
		this();
		this.token = token;
		this.newEntries = newEntries;
		this.time = Timestamp.from(Instant.now());
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<ShopOrder> getOrders() {
		return orders;
	}

	public void setOrders(List<ShopOrder> orders) {
		this.orders = orders;
	}

	public OAuth2Token getToken() {
		return token;
	}

	public void setToken(OAuth2Token token) {
		this.token = token;
	}

	public Timestamp getTime() {
		return time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	public int getNewEntries() {
		return newEntries;
	}

	public void setNewEntries(int newEntries) {
		this.newEntries = newEntries;
	}
}
