package com.salesinvoicetools.models;

import javax.persistence.*;

@Entity
public class StockItemStock {

    @Id
    @GeneratedValue
    public long id;

    @OneToOne
    @JoinColumn(name = "target_variation_id")
    public StockItemVariation targetVariation;

    public String variationValue;

    public int stock;

}
