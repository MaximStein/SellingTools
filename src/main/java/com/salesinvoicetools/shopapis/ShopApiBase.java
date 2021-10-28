package com.salesinvoicetools.shopapis;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;

import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * base class for API-implementations; create a subclass to implement specific API-support
 * getTargetShopApi function should be modified to use new implementations
 * 
 * @author Maxim Stein
 *
 */
public abstract class ShopApiBase {

	OAuth2Token token = null;
	boolean isSandbox = false;
	
	private OAuth20Service oAuth2Service = null;
	
	public ShopApiBase(OAuth2Token token, boolean isSandbox) {
		this.token = token;
		this.isSandbox = isSandbox;
		this.oAuth2Service = getOAuth2Service();
	}
	
	/**
	 * returns the api-implementation for the provided token
	 * @param token
	 * @return the api-implementation for the provided token, extending from this class
	 */
	public static ShopApiBase getTargetShopApi(OAuth2Token token) {
		switch (token.getOwner().getPlatform()) {
			case EBAY:
				return new EbayShopApi(token);
			case EBAY_FAKE:
				return new FakeEbayShopApi(token);
			default:
				throw new UnsupportedOperationException();
		}
	}


	/**
	 * refreshes the access token using the saved refreshtoken, updates the appropriate DB entry 
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean refreshToken() throws IOException, InterruptedException, ExecutionException {		
		var service = getOAuth2Service();		
		var scribeJavaToken = service.refreshAccessToken(token.getRefreshToken());				
		token.setAccessToken(scribeJavaToken.getAccessToken());
		
		if(scribeJavaToken.getRefreshToken() != null &&  !scribeJavaToken.getRefreshToken().isBlank())
			token.setRefreshToken(scribeJavaToken.getRefreshToken());
		token.setAcessTokenExpirationTime(
				Date.from(
						Instant.now().plusSeconds(scribeJavaToken.getExpiresIn())));
		
		DataAccessBase.insertOrUpdate(token);
		
		return true;
	}
	
	
	/**
	 * compares the token's saved expiration time to the current time
	 * @return
	 */
	public boolean isTokenExpired() {		
		return token.getAcessTokenExpirationTime() == null || Date.from(Instant.now()).after(token.getAcessTokenExpirationTime());
	}
	
	
	/**
	 * returns the URL for the authorization page that should be presented to the user
	 * @return the URL to direct the user to
	 */
	public String getOAuth2AuthorizationUrl() {
		final Map<String, String> additionalParams = new HashMap<>(); 
		additionalParams.put("prompt", "consent");
		return oAuth2Service.createAuthorizationUrlBuilder().build();
	} 
	
	
	/**
	 * retrieves the access token using the previously acquired access code, saves the returned token to DB if successful
	 * @param accessCode
	 * @return true if successful, otherwise false
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean tradeAccessTokenForCode(String accessCode) throws IOException, InterruptedException, ExecutionException {
		var accessToken = oAuth2Service.getAccessToken(accessCode);		

		if(accessToken == null)
			return false;
						
		System.out.println("access token: "+ accessToken.getRawResponse());				
		
		token.setAccessToken(accessToken.getAccessToken());
		token.setRefreshToken(accessToken.getRefreshToken());
		token.setAcessTokenExpirationTime(Date
				.from(Instant.now().plusSeconds(accessToken.getExpiresIn()))
			);
				
		DataAccessBase.insertOrUpdate(token.getOwner());
		
		return true;					
	}
	
	protected abstract OAuth20Service getOAuth2Service();
	
	/**
	 * subclasses should implement this function to return orders for the page and start creation time specified
	 * @param createTimeFrom only return orders later than this time
	 * @param pageNumber starting from 1
	 * @param pageSize
	 * @param outRemainingItems set this object's value to indicate how many items are left after this call
	 * @return List of ShopOrder objects
	 * @throws Exception
	 */
	public abstract List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize, SimpleIntegerProperty outRemainingItems) throws Exception;
	
	public abstract ShopOrder getOrder(String orderNumer);
	
	
}
