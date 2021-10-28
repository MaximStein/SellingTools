package com.salesinvoicetools.shopapis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.oauth.EbayApi20;
import com.salesinvoicetools.shopapis.oauth.EbaySandboxApi20;
import org.apache.commons.io.IOUtils;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;

import javafx.beans.property.SimpleIntegerProperty;

public class EbayShopApi extends ShopApiBase {

	private Gson g = new Gson();

	protected EbayShopApi(OAuth2Token t) {
		super(t, true);
	}

	protected String getBaseUrl() {
		return isSandbox ? "https://api.sandbox.ebay.com" : "https://api.ebay.com";
	}

	protected OAuth20Service getOAuth2Service() {
		var permissionString = "https://api.ebay.com/oauth/api_scope https://api.ebay.com/oauth/api_scope/sell.marketing.readonly https://api.ebay.com/oauth/api_scope/sell.marketing https://api.ebay.com/oauth/api_scope/sell.inventory.readonly https://api.ebay.com/oauth/api_scope/sell.inventory https://api.ebay.com/oauth/api_scope/sell.account.readonly https://api.ebay.com/oauth/api_scope/sell.account https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly https://api.ebay.com/oauth/api_scope/sell.fulfillment https://api.ebay.com/oauth/api_scope/sell.analytics.readonly https://api.ebay.com/oauth/api_scope/sell.finances https://api.ebay.com/oauth/api_scope/sell.payment.dispute https://api.ebay.com/oauth/api_scope/commerce.identity.readonly";

		final OAuth20Service service = new ServiceBuilder(token.getOwner().getClientId())
				.apiSecret(token.getOwner().getClientSecret()).defaultScope(permissionString)
				.callback(token.getOwner().getCallbackUrl())
				.build(isSandbox ? EbaySandboxApi20.instance() : EbayApi20.instance());
		return service;
	}

	@Override
	public List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize,
										 SimpleIntegerProperty outRemainingItems) throws Exception {

		String timeIso = ZonedDateTime.ofInstant(createTimeFrom.toInstant(), ZoneId.of("Z"))
				.format(DateTimeFormatter.ISO_INSTANT);

		var orderResult = makeHttpCall(getBaseUrl() + "/sell/fulfillment/v1/order" + "?filter=creationdate:%5B"
				+ timeIso + "..%5D&limit=" + pageSize + "&offset=" + ((pageNumber - 1) * pageSize));

		OrderSearchPagedCollection ordersResult = g.fromJson(orderResult, OrderSearchPagedCollection.class);

		List<ShopOrder> orders = new ArrayList<>();

		outRemainingItems.set(ordersResult.total - ((pageNumber - 1) * pageSize + ordersResult.orders.length));

		Arrays.stream(ordersResult.orders).forEach(order -> {

			ShippingFulfillmentPagedCollection shipping = null;

			if (order.fulfillmentHrefs.length > 0) {
				try {
					var shippingResult = makeHttpCall(
							getBaseUrl() + "/sell/fulfillment/v1/order/" + order.orderId + "/shipping_fulfillment");
					shipping = g.fromJson(shippingResult, ShippingFulfillmentPagedCollection.class);
				} catch (IOException e) {
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
		addr.setCountry(p.contactAddress.countryCode);
		addr.setPostalCode(p.contactAddress.postalCode);

		if (p.primaryPhone != null)
			addr.setPhoneNumber(p.primaryPhone.phoneNumber);

		return addr;
	}

	private ShopOrder getConvertedShopOrder(EbayOrder ebayOrder, ShippingFulfillmentPagedCollection shipping) {
		ShopOrder order = new ShopOrder();
		order.setTotalGrossAmount(Math.round(ebayOrder.pricingSummary.total.convertedFromValue * 100));
		order.setCurrencyCode(ebayOrder.pricingSummary.total.convertedFromCurrency);
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
			var products = DataAccessBase.<Product>getWhere(Product.class,
					Map.of("productNumber", t.legacyItemId, "marketplace", token.getOwner().getPlatform()));

			Product product;

			if (products.size() == 0)
				product = new Product(Math.round(t.lineItemCost.convertedFromValue / t.quantity * 100), t.title,
						t.legacyItemId, ShopOrder.Marketplace.EBAY);
			else
				product = products.get(0);

			product.setDescription(t.title);
			product.setProductNumber(t.legacyItemId);

			DataAccessBase.insertOrUpdate(product);

			item.setProduct(product);
			item.setQuantity(t.quantity);
			item.setVariation(t.legacyVariationId);
			item.setTotalPriceGross(Math.round(t.total.convertedFromValue * 100));
			order.getItems().add(item);

		}

		return order;
	}

	public void insertTestListing(Product p) throws Exception {

		if (!isSandbox)
			throw new Exception("not sandbox");

		try {

			var i = new IntenvoryItem();
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
			
			
			var content = g.toJson(i);
			
			System.out.println(content);
			
			System.out.println("============================================");
			var orderResult = makeHttpCall(getBaseUrl() + "/sell/inventory/v1/inventory_item/G123135523", "PUT",
					g.toJson(i));
			System.out.println(orderResult);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ShopOrder getOrder(String orderNumber) {

		try {
			var orderResult = makeHttpCall(getBaseUrl() + "/sell/fulfillment/v1/order/" + orderNumber);
			var shippingResult = makeHttpCall(
					getBaseUrl() + "/sell/fulfillment/v1/order/" + orderNumber + "/shipping_fulfillment");
			EbayOrder order = g.fromJson(orderResult, EbayOrder.class);
			ShippingFulfillmentPagedCollection fulfillments = g.fromJson(shippingResult,
					ShippingFulfillmentPagedCollection.class);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String makeHttpCall(String endpoint) throws IOException {
		return makeHttpCall(endpoint, null, null);
	}

	private String makeHttpCall(String endpoint, String method, String content) throws IOException {

		URL url = new URL(endpoint);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		

		http.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());
		http.setRequestProperty("Content-Language", "en-US");
		http.setRequestMethod(method == null ? "GET" : method);
		http.setDoOutput(true);
		http.setDoInput(true);

		if (content != null)
			http.setRequestProperty("Content-Type", "application/json; utf-8");

		http.connect();
		System.out.println(method+ "|"+http.getRequestMethod());
		
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
		
		
		System.out.println("[" + http.getResponseCode() + "]");
		try {

			String theString = IOUtils.toString(inputStream, "UTF-8");
			
			return theString;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	class OrderSearchPagedCollection {
		int limit;
		int total;
		String next;
		int offset;
		EbayOrder[] orders;
		ErrorDetailV3[] warnings;
	}

	class ShippingFulfillmentPagedCollection {
		ShippingFulfillment[] fulfillments;
		int total;
		ErrorDetailV3[] warnings;
	}

	class ErrorDetailV3 {
		String category;
		String domain;
		String errorId;
		String[] inputRefIds;
		String longMessage;
		String message;
		String[] outputRefIds;
		String subDomain;
	}

	class ShippingFulfillment {
		String fulfillmentId;
		LineItem[] lineItems;
		String shipmentTrackingNumber;
		Date shippedDate;
		String shippingCarrierCode;
	}

	
	class ShipToLocationAvailability {
		int quantity;
		
	}
	
	class Availability {
		ShipToLocationAvailability shipToLocationAvailability;
	}
	
	class EbayProduct {
		String brand;
		String title;
		String description;
		HashMap<String, String[]> aspects;
		String product;
		String mpn;
		String[] imageUrls;
	}
	
	class IntenvoryItem {
		Availability availability;
		String condition;
		EbayProduct product;
	}
	
	class EbayOrder {

		String orderId;
		Date creationDate;
		Date lastModifiedDate;
		String orderFulfillmentStatus;
		String orderPaymentStatus;
		String sellerId;
		String buyerCheckoutNotes;
		PricingSummary pricingSummary;
		CancelStatus cancelStatus;
		PaymentSummary paymentSummary;
		LineItem[] lineItems;
		FulfillmentStartInstruction[] fulfillmentStartInstructions;
		String[] fulfillmentHrefs;
		Buyer buyer;

		class Buyer {
			String username;
			Addr taxAddress;
		}

		class CancelStatus {
			public String cancelState;

		}

		class LineItem {
			String lineItemId;
			String legacyItemId;
			String legacyVariationId;
			String title;
			int quantity;
			Amount total;
			DeliveryCost deliveryCost;
			Amount lineItemCost;
			Amount discountedLineItemCost;
		}

		class Amount {
			double convertedFromValue;
			String convertedFromCurrency;
		}

		class DeliveryCost {
			Amount shippingCost;
		}

		class PricingSummary {
			Amount pricingSummary;
			Amount deliveryCost;
			Amount total;
		}

		class PaymentSummary {
			Amount totalDueSeller;
			Payment[] payments;
			Amount[] refunds;

		}

		class Payment {
			String paymentMethod;
			String paymentReferenceId;
			Amount amount;
			String paymentStatus;
		}

		class FulfillmentStartInstruction {
			String fulfillmentInstructionsType;
			Date minEstimatedDeliveryDate;
			Date maxEstimatedDeliveryDate;
			boolean ebaySupportedFulfillment;
			ShippingStep shippingStep;
		}

		class ShippingStep {
			String shippingCarrierCode;
			String shippingServiceCode;
			Person shipTo;
		}

		class Person {
			String fullName;
			Addr contactAddress;
			String email;
			Phone primaryPhone;
		}

		class Phone {
			String phoneNumber;
		}

		class Addr {
			String addressLine1;
			String addressLine2;
			String city;
			String postalCode;
			String countryCode;
		}

	}

	// private enum CancelState { NONE_REQUESTED };

}
