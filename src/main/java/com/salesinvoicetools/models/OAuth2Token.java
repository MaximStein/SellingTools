package com.salesinvoicetools.models;

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

	@Column(unique = true)
	public String name;

	public String accessToken;

	public String refreshToken;

	public Date acessTokenExpirationTime;

	public Date refreshTokenExpirationTime;
	
	public boolean isActive = true;

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@OneToMany(mappedBy = "token", cascade = CascadeType.ALL)
	public List<DataSource> dataSources;

	@ManyToOne(cascade = CascadeType.ALL)
	public ApiAccess owner;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Date getAcessTokenExpirationTime() {
		return acessTokenExpirationTime;
	}

	public void setAcessTokenExpirationTime(Date acessTokenExpirationTime) {
		this.acessTokenExpirationTime = acessTokenExpirationTime;
	}

	public Date getRefreshTokenExpirationTime() {
		return refreshTokenExpirationTime;
	}

	public void setRefreshTokenExpirationTime(Date refreshTokenExpirationTime) {
		this.refreshTokenExpirationTime = refreshTokenExpirationTime;
	}

	public List<DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(List<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	public ApiAccess getOwner() {
		return owner;
	}

	public void setOwner(ApiAccess owner) {
		this.owner = owner;
	}

	public OAuth2Token() {
	};

	public String toString() {
		return name;
	}

}
