package com.salesinvoicetools.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class StockItemVariation {
    @Id
    @GeneratedValue
    public long id;

    public String title;

    public boolean affectsPrice;

    public boolean affectsStock;

    public String possibleValues;

    @OneToOne
    public StockItemStock stockAmount;

    @OneToOne
    public StockItemPrice price;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private StockItem owner;

}
