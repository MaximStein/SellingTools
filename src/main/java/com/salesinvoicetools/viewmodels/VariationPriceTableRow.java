package com.salesinvoicetools.viewmodels;

import com.salesinvoicetools.models.StockItemVariation;

import java.util.List;
import java.util.Map;

public class VariationPriceTableRow {
    public  VariationPriceTableRow(){};

    public VariationPriceTableRow(Map<String, String> map, Long price){
        this.variationValues = map;
        this.price = price;
    }
    public Map<String, String> variationValues;
    public Long price;
}
