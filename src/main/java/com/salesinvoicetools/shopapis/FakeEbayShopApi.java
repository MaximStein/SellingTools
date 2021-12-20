package com.salesinvoicetools.shopapis;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.oauth.EbayApi20;
import com.salesinvoicetools.shopapis.oauth.EbaySandboxApi20;
import com.salesinvoicetools.utils.AppUtils;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;

import javafx.beans.property.SimpleIntegerProperty;


public class FakeEbayShopApi extends ShopApiBase {

	private static ArrayList<ShopOrder> orders = new ArrayList<>();

	static {
		orders = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			orders.add(getOrderTemplate(i));
		}
	}
	
	private static ShopOrder getOrderTemplate(int index) {

		Random r = new Random(index);
				
		var order = new ShopOrder();
		
		order.setOrderNumber(""+(r.nextInt() & Integer.MAX_VALUE)+"_"+index);
		
		var addr = new Address();
		addr.setName("Testname "+index);
		addr.setStreet("Reinbeker Redder "+index);
		addr.setCity("Hamburg");
		addr.setPostalCode("21031");
		addr.country = AppUtils.parseCountry("Deutschland");
		order.setBillingsAddress(addr);
		
		var randVal = r.nextInt(60 * 24 * 60 * 60 * 1000);
		
		Timestamp orderTime= new Timestamp(System.currentTimeMillis() - randVal);
		
		order.setOrderTime(orderTime);
		order.setBuyerCheckoutMessage("test checkout message "+index);

		order.setShippedTime(new Timestamp(System.currentTimeMillis() - randVal/2));
		
		order.setBuyer(new ContactInfo());
		order.getBuyer().setEmail("testemail"+index+"@test.de");
		order.getBuyer().setUserName("testuser"+index);

		order.setItems(new ArrayList<LineItem>());

		var testProduct = new Product();
		
		testProduct.setTitle("testproduct "+index);
		testProduct.setProductNumber(""+r.nextInt(Integer.MAX_VALUE));
		testProduct.grossPriceMin = testProduct.grossPriceMax = 1500l;

		var lineItem1 = new LineItem();
		lineItem1.setOwner(order);
		lineItem1.setQuantity(3);
		lineItem1.setProduct(testProduct);
		lineItem1.setTotalPriceGross(testProduct.grossPriceMax * lineItem1.getQuantity());
		order.getItems().add(lineItem1);

		order.setShippingCosts(150);
		order.setTotalGrossAmount(lineItem1.getTotalPriceGross() + order.getShippingCosts());

		return order;
	}

	
	protected FakeEbayShopApi(OAuth2Token t) {
		super(t, true);
	}

	
	protected OAuth20Service getOAuth2Service() {
		var permissionString = "https://api.ebay.com/oauth/api_scope https://api.ebay.com/oauth/api_scope/sell.marketing.readonly https://api.ebay.com/oauth/api_scope/sell.marketing https://api.ebay.com/oauth/api_scope/sell.inventory.readonly https://api.ebay.com/oauth/api_scope/sell.inventory https://api.ebay.com/oauth/api_scope/sell.account.readonly https://api.ebay.com/oauth/api_scope/sell.account https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly https://api.ebay.com/oauth/api_scope/sell.fulfillment https://api.ebay.com/oauth/api_scope/sell.analytics.readonly https://api.ebay.com/oauth/api_scope/sell.finances https://api.ebay.com/oauth/api_scope/sell.payment.dispute https://api.ebay.com/oauth/api_scope/commerce.identity.readonly";

		final OAuth20Service service = new ServiceBuilder(token.getOwner().clientId)
				.apiSecret(token.getOwner().clientSecret).defaultScope(permissionString)
				.callback(token.getOwner().callbackUrl)
				.build(isSandbox ? EbaySandboxApi20.instance() : EbayApi20.instance());
		return service;
	}

	@Override
	public List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize,
			SimpleIntegerProperty outRemainingItems) throws Exception {

		var toIndex = Math.min(orders.size(), (pageNumber-1) * pageSize + pageSize);
		
		var remainingItems = orders.size() - toIndex;
		
		outRemainingItems.set(remainingItems);
		
		Thread.sleep(400);
		
		return orders.subList(Math.min((pageNumber-1) * pageSize, orders.size()-1), toIndex);
	}

	public ShopOrder getOrder(String orderNumber) {
		return null;
	}

	@Override
	public Product getProduct(String itemId) {
		return null;
	}

	@Override
	public String getApiBaseUrl() {
		return null;
	}

}
