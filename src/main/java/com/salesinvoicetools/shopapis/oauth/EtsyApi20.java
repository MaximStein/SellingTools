package com.salesinvoicetools.shopapis.oauth;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.io.OutputStream;

public class EtsyApi20  extends DefaultApi20 {

    protected EtsyApi20() {
    }

    private static class InstanceHolder {

        private static final EtsyApi20 INSTANCE = new EtsyApi20();
    }

    public static EtsyApi20 instance() {
        return EtsyApi20.InstanceHolder.INSTANCE;
    }



    @Override
    public String getAccessTokenEndpoint() {
        return "https://api.etsy.com/v3/public/oauth/token";
    }

    @Override
    public OAuth20Service createService(String apiKey, String apiSecret, String callback, String defaultScope,
                                        String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                                        HttpClient httpClient) {
        var s = super.createService(apiKey, apiSecret,  callback, defaultScope, responseType, debugStream, userAgent, httpClientConfig, httpClient);

        return s;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://www.etsy.com/oauth/connect";
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }

}