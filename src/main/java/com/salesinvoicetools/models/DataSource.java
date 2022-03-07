package com.salesinvoicetools.models;

import java.sql.Time;
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
	public transient List<ShopOrder> orders;

	@ManyToOne(cascade = CascadeType.PERSIST)
	public OAuth2Token token;

	public Timestamp time;

	public int newEntries;

	public Timestamp getTime() {
		return this.time;
	}

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
}
