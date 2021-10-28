package com.salesinvoicetools.models;

import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.salesinvoicetools.models.ShopOrder.Marketplace;
import static java.util.Map.entry;

@Entity
public class ApiAccess {

	@Id
	@Enumerated(EnumType.STRING)
	public Marketplace platform;

	public String clientSecret;

	public String clientId;

	public String callbackUrl;

	@OneToMany(mappedBy = "owner", cascade = { CascadeType.ALL })
	public List<OAuth2Token> tokens;

	
	public ApiAccess() {
	}

	public ApiAccess(Marketplace p, String clientId, String clientSecret, String callbackUrl) {
		this.clientId = clientId;
		this.platform = p;
		this.clientSecret = clientSecret;
		this.callbackUrl = callbackUrl;
	}


	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "" + String.valueOf(platform) + "	" + clientId;
	}

	public Marketplace getPlatform() {
		return platform;
	}

	public void setPlatform(Marketplace platform) {
		this.platform = platform;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	public List<OAuth2Token> getTokens() {
		return tokens;
	}

	public void setTokens(List<OAuth2Token> tokens) {
		this.tokens = tokens;
	}
}
