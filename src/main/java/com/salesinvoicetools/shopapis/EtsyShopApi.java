package com.salesinvoicetools.shopapis;

import com.github.scribejava.core.oauth.OAuth20Service;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.utils.Pagination;
import javafx.beans.property.SimpleIntegerProperty;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

public class EtsyShopApi extends ShopApiBase {

	public EtsyShopApi(OAuth2Token token, boolean isSandbox) {
		super(token, isSandbox);
	}

	public static List<ShopOrder> getOrdersPage(Timestamp untilWhen, Pagination paginationRef) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected OAuth20Service getOAuth2Service() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ShopOrder> getOrdersPage(Calendar createTimeFrom, int pageNumber, int pageSize,
			SimpleIntegerProperty outRemainingItems) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public ShopOrder getOrder(String orderNumer) {
		throw new UnsupportedOperationException();
	}

}
