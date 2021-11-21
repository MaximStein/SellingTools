package com.salesinvoicetools.shopapis;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.sql.Timestamp;
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

import com.google.api.client.http.HttpMethods;
import com.google.gson.Gson;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.DataUpdateDataAccess;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.shopapis.oauth.EtsyApi20;
import com.salesinvoicetools.utils.AppUtils;
import com.salesinvoicetools.utils.NetUtils;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableIntegerValue;

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

	protected Gson gson = new Gson();

	private OAuth20Service oAuth2Service = null;
	
	public ShopApiBase(OAuth2Token token, boolean isSandbox) {
		this.token = token;
		this.isSandbox = isSandbox;
		this.oAuth2Service = getOAuth2Service();
	}

	public void retrieveOrders(int pastDaysMax, DoubleProperty outProgress) {
		var updates = DataUpdateDataAccess.getUpdatesForToken(token);
		Calendar timeFrom = Calendar.getInstance();
		var update = new DataSource(token, 0);

		timeFrom.add(Calendar.DAY_OF_YEAR, -pastDaysMax);

		update.setTime(Timestamp.from(Instant.now()));
		final var upd = update;

		var pageNumber = 1;
		var ordersProcessed = 0;

		try {
			SimpleIntegerProperty remainingEntries = new SimpleIntegerProperty(0);

			do {
				var ordersPage = this.getOrdersPage(timeFrom, pageNumber++, 10, remainingEntries);
				ordersPage.forEach(o -> {
					o.setDataSource(upd);

					var existing = OrderDataAccess.getByOrderNumber(o.getOrderNumber(), token.getOwner().platform);
					AppUtils.log(existing == null ? "Inserting "+o.getMarketplaceString()+"-order "+o.orderNumber : "order "+o.orderNumber+" already in DB");

					if (existing == null) {
						DataAccessBase.insertOrUpdate(o);
					}
					else {
						existing.paymentComplete = o.paymentComplete;
						existing.shippedTime = o.shippedTime;
						existing.refundAmount = o.refundAmount;
						DataAccessBase.insertOrUpdate(existing);
					}
				});

				ordersProcessed += ordersPage.size();
				var totalOrders = remainingEntries.get() + ordersProcessed;
				final double progress = 1d * ordersProcessed / totalOrders;
				outProgress.setValue(progress);

			} while (remainingEntries.get() > 0 && ordersProcessed < 10000);

			upd.setNewEntries(ordersProcessed);
			Platform.runLater(() -> {
				DataAccessBase.insertOrUpdate(upd);
				DataUpdateDataAccess.removeUnusedUpdates(token);

			//	if(finishedHandler != null)
		//			finishedHandler.run();
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * returns the api-implementation for the provided token
	 * @param token
	 * @return the api-implementation for the provided token, extending from this class
	 */
	public static ShopApiBase getTargetShopApi(OAuth2Token token) {
		switch (token.getOwner().platform) {
			case ETSY:
				return new EtsyShopApi(token);
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

	public abstract String getApiBaseUrl();

	public String makeAuthorizedApiCall(String endpoint, String method, String contentType) {

		if (isTokenExpired()) {
			try {
				refreshToken();
			} catch (IOException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return null;
			}
		}

		HttpURLConnection conn = null;
		try {
			conn = NetUtils.openHttpURLConnection(this.getApiBaseUrl()+"/"+endpoint, method, null, token.accessToken, contentType);
			var response = NetUtils.getHttpConnectionContent(conn);
			return response;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public enum Marketplace {
		EBAY_FAKE,EBAY,ETSY,OTHER
	}
}



