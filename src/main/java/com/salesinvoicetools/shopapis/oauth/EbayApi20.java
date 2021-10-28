package com.salesinvoicetools.shopapis.oauth;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;

public class EbayApi20 extends DefaultApi20 {

	protected EbayApi20() {
	}

	private static class InstanceHolder {

		private static final EbayApi20 INSTANCE = new EbayApi20();
	}

	public static EbayApi20 instance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.ebay.com/identity/v1/oauth2/token";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return "https://auth.ebay.com/oauth2/authorize";
	}

	@Override
	public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
		return OpenIdJsonTokenExtractor.instance();
	}

}
