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

    public List<String> getPossibleValuesList() {
        return possibleValues == null ? new ArrayList<String>() : List.of(possibleValues.split(","));
    }

    @ManyToOne
    @JoinColumn(name = "owner_id")
    public StockItem owner;

}
