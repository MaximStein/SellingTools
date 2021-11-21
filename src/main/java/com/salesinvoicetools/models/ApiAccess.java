package com.salesinvoicetools.models;

import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.shopapis.ShopApiBase.Marketplace;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;


import static java.util.Map.entry;

@Entity
public class ApiAccess {

	@Id
	@Enumerated(EnumType.STRING)
	public ShopApiBase.Marketplace platform;

	public String clientSecret;

	public String clientId;

	public String callbackUrl;

	@OneToMany(mappedBy = "owner", cascade = { CascadeType.ALL })
	public List<OAuth2Token> tokens;

	public String customData;

	public ApiAccess() {
	}

	public ApiAccess(Marketplace p, String clientId, String clientSecret, String callbackUrl) {
		this.clientId = clientId;
		this.platform = p;
		this.clientSecret = clientSecret;
		this.callbackUrl = callbackUrl;
	}

	@Override
	public String toString() {		return "" + String.valueOf(platform) + "		 " + clientId; 	}

}
