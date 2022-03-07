package com.salesinvoicetools.viewmodels;

import com.salesinvoicetools.models.StockItem;
import com.salesinvoicetools.models.StockItemVariation;
import com.salesinvoicetools.shopapis.etsy.Pojos;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockItemDetailsModel {

    public StockItem item;

    public SimpleStringProperty title;

    public SimpleStringProperty description;

    public ObservableList<VariatiationModel> variations;
    public StockItemDetailsModel() {}

    public StockItemDetailsModel(StockItem item) {
        this.item = item;

        title = new SimpleStringProperty(item.title);

        description = new SimpleStringProperty(item.description);

        this.variations = FXCollections.observableList(item.variations
                .stream()
                .map(var -> new VariatiationModel(var)).collect(Collectors.toList()));
    }

    public class VariatiationModel {

        public VariatiationModel(StockItemVariation variation){
            this.variation = variation;

            this.affectsPrice = new SimpleBooleanProperty(variation.affectsPrice);
            this.affectsStock = new SimpleBooleanProperty(variation.affectsStock);
            this.possibleValues = new SimpleStringProperty(variation.possibleValues);
            this.name = new SimpleStringProperty(variation.title);
        }

        public StockItemVariation variation;

        public SimpleBooleanProperty affectsPrice;

        public SimpleBooleanProperty affectsStock;

        public SimpleStringProperty possibleValues;

        public StringProperty name;
    }
}
