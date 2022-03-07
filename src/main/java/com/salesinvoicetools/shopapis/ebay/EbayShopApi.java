package com.salesinvoicetools.shopapis.ebay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.google.api.client.util.Strings;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.ProductsDataAccess;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.shopapis.ebay.Pojos.*;
import com.salesinvoicetools.shopapis.oauth.EbayApi20;
import com.salesinvoicetools.shopapis.oauth.EbaySandboxApi20;
import com.salesinvoicetools.utils.AppUtils;
import org.apache.commons.io.IOUtils;


import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;

import javafx.beans.property.SimpleIntegerProperty;

public class EbayShopApi extends ShopApiBase {


	public EbayShopApi(OAuth2Token t) {
		super(t, false);
	}

	public String getApiBaseUrl() {
		return isSandbox ? "https://api.sandbox.ebay.com" : "https://api.ebay.com";
	}

	private static final String scopes="https://api.ebay.com/oauth/api_scope https://api.ebay.com/oauth/api_scope/sell.marketing.readonly https://api.ebay.com/oauth/api_scope/sell.marketing https://api.ebay.com/oauth/api_scope/sell.inventory.readonly https://api.ebay.com/oauth/api_scope/sell.inventory https://api.ebay.com/oauth/api_scope/sell.account.readonly https://api.ebay.com/oauth/api_scope/sell.account https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly https://api.ebay.com/oauth/api_scope/sell.fulfillment https://api.ebay.com/oauth/api_scope/sell.analytics.readonly https://api.ebay.com/oauth/api_scope/sell.finances https://api.ebay.com/oauth/api_scope/sell.payment.dispute https://api.ebay.com/oauth/api_scope/commerce.identity.readonly";

	protected OAuth20Service getOAuth2Service() {
		var permissionString = "https://api.ebay.com/oauth/api_scope https://api.ebay.com/oauth/api_scope/sell.marketing.readonly https://api.ebay.com/oauth/api_scope/sell.marketing https://api.ebay.com/oauth/api_scope/sell.inventory.readonly https://api.ebay.com/oauth/api_scope/sell.inventory https://api.ebay.com/oauth/api_scope/sell.account.readonly https://api.ebay.com/oauth/api_scope/sell.account https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly https://api.ebay.com/oauth/api_scope/sell.fulfillment https://api.ebay.com/oauth/api_scope/sell.analytics.readonly https://api.ebay.com/oauth/api_scope/sell.finances https://api.ebay.com/oauth/api_scope/sell.payment.dispute https://api.ebay.com/oauth/api_scope/commerce.identity.readonly";

		final OAuth20Service service = new ServiceBuilder(token.owner.clientId)
				.apiSecret(token.owner.clientSecret).defaultScope(permissionString)
				.callback(token.owner.callbackUrl)

				.build(isSandbox ? EbaySandboxApi20.instance() : EbayApi20.instance());
		return service;
	}

	@Override
	public List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize,
										 SimpleIntegerProperty outRemainingItems) throws Exception {

		String timeIso = ZonedDateTime.ofInstant(createTimeFrom.toInstant(), ZoneId.of("Z"))
				.format(DateTimeFormatter.ISO_INSTANT);
		var orderResult = makeHttpCall(getApiBaseUrl() + "/sell/fulfillment/v1/order" + "?filter=creationdate:%5B"
				+ timeIso + "..%5D&limit=" + pageSize + "&offset=" + ((pageNumber - 1) * pageSize));

		Pojos.OrderSearchPagedCollection ordersResult = gson.fromJson(orderResult, Pojos.OrderSearchPagedCollection.class);
		List<ShopOrder> orders = new ArrayList<>();

		if(ordersResult == null) {
			AppUtils.log("=== API call failed ===");
			return orders;
		}

		if(ordersResult.orders == null) {
			AppUtils.log("=== no orders returned ===");
			return orders;
		}

		outRemainingItems.set(ordersResult.total - ((pageNumber - 1) * pageSize + ordersResult.orders.length));

		Arrays.stream(ordersResult.orders).forEach(order -> {
			Pojos.ShippingFulfillmentPagedCollection shipping = null;
			if (order.fulfillmentHrefs.length > 0) {
				try {
					var shippingResult = makeHttpCall(
							getApiBaseUrl() + "/sell/fulfillment/v1/order/" + order.orderId + "/shipping_fulfillment");
					shipping = gson.fromJson(shippingResult, Pojos.ShippingFulfillmentPagedCollection.class);
				} catch (IOException | ExecutionException | InterruptedException e) {
					e.printStackTrace();
				}
			}
			orders.add(getConvertedShopOrder(order, shipping));
		});
		return orders;
	}

	private Address getConvertedAddress(EbayOrder.Person p) {

		if (p == null || p.contactAddress == null)
			return null;

		var addr = new Address();
		addr.setName(p.fullName);
		addr.setCity(p.contactAddress.city);
		addr.setStreet(p.contactAddress.addressLine1);
		addr.setAdditionalInfo(p.contactAddress.addressLine2);
		addr.country = AppUtils.parseCountry(p.contactAddress.countryCode);
		addr.setPostalCode(p.contactAddress.postalCode);

		if (p.primaryPhone != null)
			addr.setPhoneNumber(p.primaryPhone.phoneNumber);

		return addr;
	}

	private ShopOrder getConvertedShopOrder(Pojos.EbayOrder ebayOrder, Pojos.ShippingFulfillmentPagedCollection shipping) {
		ShopOrder order = new ShopOrder();
		order.totalGrossAmount = Math.round(ebayOrder.pricingSummary.total.value * 100);
		order.setCurrencyCode(ebayOrder.pricingSummary.total.currency);
		order.setOrderNumber(ebayOrder.orderId);
		order.setPaymentComplete(ebayOrder.orderPaymentStatus.equals("PAID"));
		order.setBuyerCheckoutMessage(ebayOrder.buyerCheckoutNotes);
		order.setShippingAddress(getConvertedAddress(ebayOrder.fulfillmentStartInstructions[0].shippingStep.shipTo));
		order.setBillingsAddress(getConvertedAddress(ebayOrder.fulfillmentStartInstructions[0].shippingStep.shipTo));
		if (shipping != null && shipping.fulfillments != null && shipping.fulfillments.length > 0) {
			order.setShippedTime(shipping.fulfillments[0].shippedDate == null ? null
					: Timestamp.from(shipping.fulfillments[0].shippedDate.toInstant()));
		}
		order.setOrderTime(Timestamp.from(ebayOrder.creationDate.toInstant()));
		order.setItems(new ArrayList<>());
		order.setBuyer(new ContactInfo(ebayOrder.buyer.username));

		for (var t : ebayOrder.lineItems) {
			var item = new LineItem();
			item.setOwner(order);

			Product product = ProductsDataAccess.getNewOrExisting(Marketplace.EBAY, t.legacyItemId);
			product.grossPriceMin = product.grossPriceMax = Math.round(t.lineItemCost.value / t.quantity * 100);
			product.title = t.title;

			if(product.imageUrls == null || Strings.isNullOrEmpty(product.imageUrls.trim())) {
				var apiProduct = getProduct(t.legacyItemId);
				if(apiProduct != null) {
					product.imageUrls = apiProduct.imageUrls;
				}
			}

			DataAccessBase.insertOrUpdate(product);

			item.setProduct(product);
			item.setQuantity(t.quantity);
			item.setVariation(t.legacyVariationId);
			item.setTotalPriceGross(Math.round(t.total.value * 100));
			order.getItems().add(item);
		}

		return order;
	}



	public void insertTestListing(Product p) throws Exception {

		if (!isSandbox)
			throw new Exception("not sandbox");

		try {

			var i = new Pojos.IntenvoryItem();
			i.availability = new Availability();
			i.availability.shipToLocationAvailability = new ShipToLocationAvailability();
			i.availability.shipToLocationAvailability.quantity = 50;
			i.condition = "NEW";
			i.product = new EbayProduct();
			i.product.description = "testdescription";
			i.product.title = "testtitle";
			i.product.brand = "GoPro";		
			i.product.mpn = "CHDHX-401";
			i.product.aspects = new HashMap<>();
			i.product.aspects.put("Brand", new String[] {"GoPro"});
			i.product.imageUrls = new String[] { "https://i.ebayimg.com/images/g/~QwAAOSwUfReZmHh/s-l1600.jpg "};
			
			
			var content = gson.toJson(i);
			
			System.out.println(content);
			
			System.out.println("============================================");
			var orderResult = makeHttpCall(getApiBaseUrl() + "/sell/inventory/v1/inventory_item/G123135523", "PUT",
					gson.toJson(i));
			System.out.println(orderResult);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ShopOrder getOrder(String orderNumber) {

		try {
			var orderResult = makeHttpCall(getApiBaseUrl() + "/sell/fulfillment/v1/order/" + orderNumber);
			var shippingResult = makeHttpCall(
					getApiBaseUrl() + "/sell/fulfillment/v1/order/" + orderNumber + "/shipping_fulfillment");
			EbayOrder order = gson.fromJson(orderResult, EbayOrder.class);
			ShippingFulfillmentPagedCollection fulfillments = gson.fromJson(shippingResult,
					ShippingFulfillmentPagedCollection.class);

		} catch (IOException | ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public Product getProduct(String itemId) {
		try {
			var content = this.makeHttpCall(getApiBaseUrl()+"/buy/browse/v1/item/get_item_by_legacy_id?legacy_item_id="+itemId);
			var item = gson.fromJson(content, EbayOrder.Item.class);
			var error = gson.fromJson(content, EbayOrder.ErrorResponse.class);
			var p = new Product();
			p.productNumber = itemId;

			if(item.title == null) {
				if(error.errors.get(0).errorId == 11006) {
					content = makeHttpCall(getApiBaseUrl()+"/buy/browse/v1/item/get_items_by_item_group?item_group_id="+itemId);
					var group = gson.fromJson(content, EbayOrder.ItemGroup.class);

					if(group == null) {
						return null;
					}

					var prices = group.items.stream().map(item1 -> Math.round(item1.price.value * 100));

					var maxPrice = group.items.stream().map(item1 -> Math.round(item1.price.value * 100)).max(Long::compareTo).get();
					var minPrice = group.items.stream().map(item1 -> Math.round(item1.price.value * 100)).min(Long::compareTo).get();

					p.grossPriceMax = maxPrice;
					p.grossPriceMin = minPrice;
				}
			}
			else {

				System.out.println(content);
				p.imageUrls = item.image.imageUrl;
				p.marketplace = Marketplace.EBAY;
				p.grossPriceMin = p.grossPriceMax = Math.round(item.price.value * 100);
				p.title = item.title;
				//p.customData = content;
				//p.imageUrls
				return p;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}


	@Override
	public String getOAuth2AuthorizationUrl() {

		String oAuth2AuthorizationUrl = super.getOAuth2AuthorizationUrl();

		var url ="https://auth.ebay.com/oauth2/authorize?" +
				"client_id="+this.token.owner.clientId +
				"&redirect_uri="+this.token.owner.callbackUrl+"&" +
				"response_type=code&" +
				"state=1&" +
				"scope="+ URLEncoder.encode(scopes)+"&" +
				"prompt=login";

		return url;
	}

	private String makeHttpCall(String endpoint) throws IOException, ExecutionException, InterruptedException {
		return makeHttpCall(endpoint, null, null);
	}

	private String makeHttpCall(String endpoint, String method, String content) throws IOException, ExecutionException, InterruptedException {

		URL url = new URL(endpoint);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		
		if(isTokenExpired()) {
			refreshToken();
		}

		http.setRequestProperty("Authorization", "Bearer " + token.accessToken);
		http.setRequestProperty("Content-Language", "en-US");
		http.setRequestMethod(method == null ? "GET" : method);
		http.setDoOutput(true);
		http.setDoInput(true);

		if (content != null)
			http.setRequestProperty("Content-Type", "application/json; utf-8");

		http.connect();
		
		if (content != null) {
			try (OutputStream os = http.getOutputStream()) {
				byte[] input = content.getBytes("utf-8");
				os.write(input, 0, input.length);
				os.close();
			}
		} else {
		}
		InputStream inputStream = null;     
		try {
		    inputStream = http.getInputStream();
		} catch(IOException exception) {
		   inputStream = http.getErrorStream();
		}

		try {
			String theString = IOUtils.toString(inputStream, "UTF-8");
			return theString;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}


	// private enum CancelState { NONE_REQUESTED };

}
