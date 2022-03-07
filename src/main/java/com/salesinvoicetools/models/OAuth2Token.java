package com.salesinvoicetools.models;

import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class OAuth2Token {

	@Id
	@GeneratedValue
	public long id;

	@Expose
	@Column(unique = true)
	public String name;

	@Expose
	public String accessToken;

	@Expose
	public String refreshToken;

	@Expose
	public Date acessTokenExpirationTime;

	@Expose
	public Date refreshTokenExpirationTime;

	@Expose
	public boolean isActive = true;

	@Expose
	@OneToMany(mappedBy = "token", cascade = CascadeType.ALL)
	public List<DataSource> dataSources;

	@ManyToOne(cascade = CascadeType.ALL)
	public ApiAccess owner;

	public OAuth2Token() {
	};

	public String toString() {
		return name;
	}

}
