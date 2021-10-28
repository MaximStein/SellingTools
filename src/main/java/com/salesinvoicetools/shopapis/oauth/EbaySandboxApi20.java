package com.salesinvoicetools.shopapis.oauth;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;

public class EbaySandboxApi20 extends DefaultApi20 {

	@Override
	public String getRefreshTokenEndpoint() {
		return "https://api.sandbox.ebay.com/identity/v1/oauth2/token";
	}

	protected EbaySandboxApi20() {
	}

	private static class InstanceHolder {
		private static final EbaySandboxApi20 INSTANCE = new EbaySandboxApi20();
	}

	public static EbaySandboxApi20 instance() {
		return InstanceHolder.INSTANCE;
	}
	
	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.sandbox.ebay.com/identity/v1/oauth2/token";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return "https://auth.sandbox.ebay.com/oauth2/authorize";
	}

	@Override
	public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
		return OpenIdJsonTokenExtractor.instance();
	}

}
