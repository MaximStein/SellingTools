package com.salesinvoicetools.shopapis;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.api.client.util.Strings;
import com.google.api.client.util.escape.PercentEscaper;
import com.google.api.client.util.escape.UnicodeEscaper;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.ProductsDataAccess;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.oauth.EtsyApi20;
import com.salesinvoicetools.utils.AppUtils;
import com.salesinvoicetools.utils.ConsoleColors;
import com.salesinvoicetools.utils.NetUtils;
import com.salesinvoicetools.utils.Pagination;
import javafx.beans.property.SimpleIntegerProperty;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.salesinvoicetools.shopapis.ShopApiBase.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EtsyShopApi extends ShopApiBase {
	public static String LOG_COLOR = "CYAN";

	private static String CODE_CHALLENGE = "DSWlW2Abh-cf8CeLL8-g3hQ2WQyYdKyiu83u_s7nRhI";
	private static String CODE_VERIFIER  = "vvkdljkejllufrvbhgeiegrnvufrhvrffnkvcknjvfid";
	private static String CURRENT_VERIFIER = null;

	public EtsyShopApi(OAuth2Token token) {
		super(token, false);
	}

	public static List<ShopOrder> getOrdersPage(Timestamp untilWhen, Pagination paginationRef) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getApiBaseUrl() {
		return "https://openapi.etsy.com/v3/application";
	}

	public String getShopByUserId(String userId) {
		try {
			var response = NetUtils.makeHttpCall(getApiBaseUrl()+"/users/"+userId+"/shops","GET", null, token.accessToken, null);

			return response;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean refreshToken() throws IOException, InterruptedException, ExecutionException {

		var params = "grant_type=refresh_token&client_id="
				+token.getOwner().clientSecret+"&refresh_token="+token.refreshToken;

		var conn = NetUtils.openHttpURLConnection(EtsyApi20.instance().getAccessTokenEndpoint(),  "POST", params,null,"application/x-www-form-urlencoded" );
		var response = NetUtils.getHttpConnectionContent(conn);
		var accessTokenResponse = gson.fromJson(response, OAuth2TokenResponse.class);

		if(conn.getResponseCode() != 200 || Strings.isNullOrEmpty(accessTokenResponse.access_token))
			return false;

		token.refreshToken = accessTokenResponse.refresh_token;
		token.accessToken = accessTokenResponse.access_token;
		token.setAcessTokenExpirationTime(
				Date.from(
						Instant.now().plusSeconds(accessTokenResponse.expires_in)));

		DataAccessBase.insertOrUpdate(token);
		return true;
	}

	@Override
	protected OAuth20Service getOAuth2Service() {
		var permissionString = "listings_r address_r profile_r shops_r transactions_r email_r";

		final OAuth20Service service = new ServiceBuilder(token.getOwner().clientId)
				.apiSecret(token.getOwner().clientSecret)
				.defaultScope(permissionString)
				.callback("https://localhost")
				//.callback(token.getOwner().getCallbackUrl())
				.build(isSandbox ? EtsyApi20.instance() : EtsyApi20.instance());
		return service;
	}

	public ReceiptResponse getShopReceipts(Calendar timeFrom, int offset, int limit, boolean onlyPaid) {

		var shopResponse = getShop();
		if(shopResponse == null)
			return null;

		var url = "shops/"+shopResponse.shop_id+"/receipts?min_created="+ timeFrom.toInstant().getEpochSecond()+"&limit="+limit+"&offset="+offset+"&client_id="+token.owner.clientSecret;
		AppUtils.log(url);

		var content = this.makeAuthorizedApiCall(url, null, null);

		if(content == null)
			return null;

		var response = gson.fromJson(content, ReceiptResponse.class);

		System.out.println("retrieved "+response.count+" receipts from ETSY");

		return response;
	}

	@Override
	public List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize,
			SimpleIntegerProperty outRemainingItems) throws Exception {

		ReceiptResponse receipts = getShopReceipts(createTimeFrom, (pageNumber - 1) * pageSize, pageSize,false);

		AppUtils.log("retrieved "+receipts.results.size()+" of "+receipts.count+" receipts (page "+
				pageNumber+", order time from "+createTimeFrom.toString()+")");

		AppUtils.log("remaining items: "+(outRemainingItems.get() - receipts.results.size()));

		var orders = new ArrayList<ShopOrder>();

		var dataSource = new DataSource(token);

		if(pageNumber == 1)
			outRemainingItems.set(receipts.count);

		int i = 0;

		receipts.results.forEach(r -> {
			var order = new ShopOrder();
			AppUtils.log("processing receipt: "+r, 1);
			order.totalGrossAmount = r.grandtotal.amount;
			order.orderNumber = ""+r.receipt_id;
			order.dataSource = dataSource;
			order.shippingCosts = r.total_shipping_cost.amount;
			order.orderTime = new Timestamp(r.create_timestamp * 1000);
			order.shippedTime = r.is_shipped ? new Timestamp(r.transactions.get(0).shipped_timestamp * 1000) : null;
			order.paymentComplete = r.is_paid;

			order.buyer = new ContactInfo();
			order.buyer.userName = ""+r.buyer_user_id;
			order.buyer.email = r.buyer_email;
			order.buyerCheckoutMessage = r.message_from_buyer;
			if(!Strings.isNullOrEmpty(r.gift_message)) {
				order.buyerCheckoutMessage += "\r\n---------\r\nGeschenknachricht: "+r.gift_message;
			}

			var a = new Address();
			order.shippingAddress = a;
			a.name = r.name;
			a.street = r.first_line;
			a.additionalInfo = r.second_line;
			a.city = r.city;
			a.postalCode = r.zip;
			a.country = AppUtils.parseCountry(r.country_iso);
			order.billingsAddress = order.shippingAddress;


			r.transactions.forEach(t -> {
				//AppUtils.log("processing transaction: "+t, 2);
				var item = new LineItem();
				item.owner = order;
				item.totalPriceGross = t.price.amount * t.quantity;
				item.variation = t.variations.size() > 0 ? "" : null;

				String.join(",",
					t.variations.stream()
							.map(v -> item.variation+=v.formatted_name+":"+v.formatted_value )
							.toList());

				var p = ProductsDataAccess.getNewOrExisting(Marketplace.ETSY, ""+t.product_id);

				p.description = t.title;
				//p.description = t.description;
				DataAccessBase.insertOrUpdate(p);

				item.product = p;
				item.quantity = t.quantity;
				order.items.add(item);
			});

			orders.add(order);
		});

		outRemainingItems.set(outRemainingItems.get()-receipts.results.size());

		return orders;
	}

	@Override
	public String getOAuth2AuthorizationUrl() {

		String sha256hex = null;

		Hasher hasher = Hashing.sha256().newHasher();
		hasher.putString(CODE_VERIFIER, Charsets.UTF_8);
		HashCode sha256 = hasher.hash();

		//System.out.println("veri="+AppUtils.encodeBase64(AppUtils.toHexString(sha256.asBytes())));

		String oAuth2AuthorizationUrl = super.getOAuth2AuthorizationUrl();

		var authUrl = oAuth2AuthorizationUrl
				+"&state=asdf&code_challenge_method=S256&code_challenge="+CODE_CHALLENGE;
		return authUrl;
	}

	@Override
	public boolean tradeAccessTokenForCode(String accessCode) throws IOException, InterruptedException, ExecutionException {

		UnicodeEscaper basicEscaper = new PercentEscaper("-", false);
		//String s = basicEscaper.escape(params);
		var params = "grant_type=authorization_code&client_id="
				+token.getOwner().clientId+"&redirect_uri="
				+basicEscaper.escape("https://localhost")
				+"&code="+accessCode+"&code_verifier="+CODE_VERIFIER;

		String s = params;
		var conn = NetUtils.openHttpURLConnection(EtsyApi20.instance().getAccessTokenEndpoint(),  "POST", s,null,"application/x-www-form-urlencoded" );
		var response = NetUtils.getHttpConnectionContent(conn);
		var accessTokenResponse = gson.fromJson(response, OAuth2TokenResponse.class);

		if(conn.getResponseCode() != 200 || Strings.isNullOrEmpty(accessTokenResponse.access_token))
			return false;

		token.setAccessToken(accessTokenResponse.access_token);
		token.setRefreshToken(accessTokenResponse.refresh_token);
		token.setAcessTokenExpirationTime(Date
				.from(Instant.now().plusSeconds(accessTokenResponse.expires_in))
		);

		return true;
	}

	private Shop getShop() {
		var shops = getShops(token.owner.clientId);
		AppUtils.log( "shops found on etsy for shopname "+token.owner.clientId+":"+shops.count);
		return getShops(token.owner.clientId).results.get(0);
	}

	private ShopsResponse getShops(String shopName) {
		var response = this.makeAuthorizedApiCall("shops?shop_name="+shopName+"&limit=100&client_id="+token.owner.clientSecret, "GET", null);
		//System.out.println(response);
		return gson.fromJson(response, ShopsResponse.class);
	}

	@Override
	public ShopOrder getOrder(String orderNumer) {
		throw new UnsupportedOperationException();
	}

	public void ping() {
		try {
			var content = NetUtils.makeHttpCall(getApiBaseUrl(),null,null,null,null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	class OAuth2TokenResponse {
		public String access_token;
		public String token_type;
		public long expires_in;
		public String refresh_token;
	}

	class ReceiptResponse {
		public int count;
		public List<Receipt> results;
	}

	class ShopsResponse {
		public int count;
		public List<Shop> results;
	}

	public class MoneyAmount{
		public long amount;
		public long divisor;
		public String currency_code;

		@Override
		public String toString() {
			return AppUtils.formatCurrencyAmount(amount);
		}
	}

	public class Variation{
		public long property_id;
		public long value_id;
		public String formatted_name;
		public String formatted_value;
	}

	public class Transaction{

		@Override
		public String toString() {
			return "* "+quantity+" x "+price+" "+" | "+title+" *";
		}

		public long transaction_id;
		public String title;
		public String description;
		public long seller_user_id;
		public long buyer_user_id;
		public long create_timestamp;
		public long paid_timestamp;
		public long shipped_timestamp;
		public long quantity;
		public long listing_image_id;
		public long receipt_id;
		public boolean is_digital;
		public String file_data;
		public long listing_id;
		public String transaction_type;
		public long product_id;
		public String sku;
		public MoneyAmount price;
		public MoneyAmount shipping_cost;
		public List<Variation> variations;
		public long shipping_profile_id;
		public long min_processing_days;
		public long max_processing_days;
		public String shipping_method;
		public String shipping_upgrade;
		public long expected_ship_date;
	}

	public class Receipt{

		@Override
		public String toString() {
			return "*"+buyer_email+" : "+grandtotal+"*";
		}

		public long receipt_id;
		public long receipt_type;
		public long seller_user_id;
		public String seller_email;
		public long buyer_user_id;
		public String buyer_email;
		public String name;
		public String first_line;
		public String second_line;
		public String city;
		public String state;
		public String zip;
		public String status;
		public String formatted_address;
		public String country_iso;
		public String payment_method;
		public String payment_email;
		public String message_from_seller;
		public String message_from_buyer;
		public String message_from_payment;
		public boolean is_paid;
		public boolean is_shipped;
		public long create_timestamp;
		public long update_timestamp;
		public String gift_message;
		public MoneyAmount grandtotal;
		public MoneyAmount subtotal;
		public MoneyAmount total_price;
		public MoneyAmount total_shipping_cost;
		public MoneyAmount total_tax_cost;
		public MoneyAmount total_vat_cost;
		public MoneyAmount discount_amt;
		public MoneyAmount gift_wrap_price;
		public List<Object> shipments;
		public List<Transaction> transactions;
	}

	public class Shop{
		public long shop_id;
		public long user_id;
		public String shop_name;
		public long create_date;
		public String title;
		public String announcement;
		public String currency_code;
		public boolean is_vacation;
		public String vacation_message;
		public String sale_message;
		public String digital_sale_message;
		public long update_date;
		public long listing_active_count;
		public long digital_listing_count;
		public String login_name;
		public boolean accepts_custom_requests;
		public String policy_welcome;
		public String policy_payment;
		public String policy_shipping;
		public String policy_refunds;
		public String policy_additional;
		public String policy_seller_info;
		public long policy_update_date;
		public boolean policy_has_private_receipt_info;
		public boolean has_unstructured_policies;
		public String policy_privacy;
		public String vacation_autoreply;
		public String url;
		public String image_url_760x100;
		public long num_favorers;
		public List<String> languages;
		public String icon_url_fullxfull;
		public boolean is_using_structured_policies;
		public boolean has_onboarded_structured_policies;
		public boolean include_dispute_form_link;
		public boolean is_direct_checkout_onboarded;
		public boolean is_etsy_payments_onboarded;
		public boolean is_calculated_eligible;
		public boolean is_opted_in_to_buyer_promise;
		public boolean is_shop_us_based;
		public long transaction_sold_count;
		public String shipping_from_country_iso;
		public String shop_location_country_iso;
		public long review_count;
		public double review_average;
	}
}
