package com.salesinvoicetools.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class StockItem {
    @Id
    @GeneratedValue
    public long id;

    public String title;

    public String subtitle;

    public String description;

    public String htmlDescription;

    @OneToMany(mappedBy = "owner", cascade = { CascadeType.ALL })
    public List<StockItemVariation> variations = new ArrayList<>();

    public String customData;
}
