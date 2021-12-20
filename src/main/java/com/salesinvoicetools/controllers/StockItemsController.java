package com.salesinvoicetools.controllers;

import com.salesinvoicetools.models.StockItemStock;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.viewmodels.StockItemTableRow;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public class StockItemsController {

    @FXML
    TableView<StockItemTableRow> stockItemsTable;

    @FXML
    TableColumn<StockItemTableRow, String> titleCol;

    @FXML
    TableColumn<StockItemTableRow, String> priceCol;

    @FXML
    TableColumn<StockItemTableRow, String> imageCol;

    @FXML
    TableColumn<StockItemTableRow, Long> stockCol;

    @FXML
    TableColumn<StockItemTableRow, ShopApiBase.Marketplace[]> liveCol;

    @FXML
    TableColumn<StockItemTableRow, Void> actionCol;


    public void initialize() {

    }
}
