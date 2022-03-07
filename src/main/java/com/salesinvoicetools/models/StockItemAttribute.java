package com.salesinvoicetools.models;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "ID", "TARGET_STOCK_ITEM_ID", "TARGET_VARIATION", "ATTRIBUTE_KEY" }) })
public class StockItemAttribute {

    @Id
    @GeneratedValue
    public long id;

    @ManyToOne
    @JoinColumn(name = "target_stock_item_id")
    public StockItem targetStockItem;

    /* {farbe:"blau", größe:"m"} */
    public String targetVariation;

    public String attributeKey;

    public String attributeVal;

    public Long getLong(){
        return attributeVal == null || attributeVal.equals("null") ? null : Long.parseLong(attributeVal);
    }

    public Double getDouble() {
        return Double.parseDouble(attributeVal);
    }
}
