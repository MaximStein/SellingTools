package com.salesinvoicetools.viewmodels;

import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.models.StockItem;
import com.salesinvoicetools.models.StockItemStock;
import javafx.beans.property.SimpleBooleanProperty;

public class StockItemTableRow {
    public SimpleBooleanProperty selectedProperty;
    public StockItem item;

    public StockItemTableRow(StockItem i, boolean isSelected) {
        this.item = i;
        this.selectedProperty = new SimpleBooleanProperty(isSelected);
    }
}