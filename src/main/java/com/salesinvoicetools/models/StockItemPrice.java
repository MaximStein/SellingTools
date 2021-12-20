package com.salesinvoicetools.models;

import javax.persistence.*;

@Entity
public class StockItemPrice {

    @Id
    @GeneratedValue
    public long id;

    @OneToOne
    @JoinColumn(name = "target_variation_id")
    public StockItemVariation targetVariation;

    public String variationValue;

    public long price;

}
