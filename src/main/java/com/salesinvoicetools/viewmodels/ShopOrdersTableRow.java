package com.salesinvoicetools.viewmodels;

import com.salesinvoicetools.models.ShopOrder;
import javafx.beans.property.SimpleBooleanProperty;


public class ShopOrdersTableRow {
	public ShopOrdersTableRow(ShopOrder order, boolean isSelected) {
		this.order = order;
		this.selectedProperty = new SimpleBooleanProperty(isSelected);
	}
	
	public SimpleBooleanProperty selectedProperty;
	public ShopOrder order;
}
