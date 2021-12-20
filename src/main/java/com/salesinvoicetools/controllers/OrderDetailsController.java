package com.salesinvoicetools.controllers;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import com.google.api.client.util.Strings;

import com.salesinvoicetools.models.LineItem;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.Product;
import com.salesinvoicetools.utils.AppUtils;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.LongStringConverter;
import javafx.util.converter.NumberStringConverter;
import com.salesinvoicetools.models.Address;
import com.salesinvoicetools.models.ShopOrder;

public class OrderDetailsController {

	@FXML
	Label detailTitleLabel;

	@FXML
	AddressController billingAddressController;


	@FXML
	AddressController deliveryAddressController;

	@FXML
	private TableView<LineItem> orderItemsTable;

	@FXML
	private CheckBox deliveryAddressCheckbox;

	@FXML
	private TableColumn<LineItem, String> itemNumberColumn;

	@FXML
	private TableColumn<LineItem, String> itemDescriptionColumn;

	@FXML
	private TableColumn<LineItem, Long> itemQuantityColumn;

	@FXML
	private TableColumn<LineItem, Number> itemPricePerItemColumn;

	@FXML
	private TableColumn<LineItem, Number> itemTotalPriceColumn;

	@FXML
	private TextField vatInput;

	@FXML
	private TextField shippingInput;

	@FXML
	private TextField currencyInput;
	
	@FXML
	private TextField orderNumberInput;

	@FXML
	private DatePicker orderDateInput;

	@FXML
	private DatePicker billingDateInput;

	@FXML
	private Label marketplaceLabel;

	@FXML
	private Label grossAmountLabel;

	@FXML
	private Label netAmountLabel;

	@FXML
	private Label errorLabel;

	@FXML
	private Label detailSubtitleLabel;

	private ObservableList<LineItem> items;

	private ShopOrder shopOrder;

	private Stage stage;

	public OrderDetailsController() {	}

	public OrderDetailsController(ShopOrder order, Stage stage) {
		this.stage = stage;

		if (order == null)
			this.shopOrder = new ShopOrder();
		else
			this.shopOrder = order;
	}

	public void initialize() {

		if (shopOrder.getId() > 0)
			detailTitleLabel.setText(shopOrder.getMarketplaceString() + "-Bestellung " + shopOrder.getOrderNumber());
		else
			detailTitleLabel.setText("Neue Bestellung anlegen");

		if (shopOrder.getBuyer() != null && !Strings.isNullOrEmpty(shopOrder.getBuyer().getUserName())) {
			detailSubtitleLabel.setText("von " + shopOrder.getBuyer().getUserName());
		} else {
			detailSubtitleLabel.setText("");
		}

		items = FXCollections.observableList(shopOrder.getItems());

		orderItemsTable.setItems(items);

		itemDescriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		itemDescriptionColumn
				.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getProduct().getTitle()));
		itemDescriptionColumn.setOnEditCommit(this::editCommit);

		itemTotalPriceColumn
				.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter(Locale.GERMAN)));
		itemTotalPriceColumn
				.setCellValueFactory(item -> new SimpleObjectProperty<>(item.getValue().getTotalPriceGross() / 100d));
		itemTotalPriceColumn.setOnEditCommit(this::editCommit);

		itemPricePerItemColumn
				.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter(Locale.GERMAN)));
		itemPricePerItemColumn.setCellValueFactory(item -> new SimpleObjectProperty<>(
				Math.round(item.getValue().getTotalPriceGross() / item.getValue().getQuantity()) / 100d));
		itemPricePerItemColumn.setOnEditCommit(this::editCommit);

		itemQuantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter()));
		itemQuantityColumn
				.setCellValueFactory(item -> new SimpleLongProperty(item.getValue().getQuantity()).asObject());
		itemQuantityColumn.setOnEditCommit(this::editCommit);

		if (shopOrder.getShippingAddress() == null)
			shopOrder.setShippingAddress(new Address());

		deliveryAddressCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				deliveryAddressController.setDisabled(newValue);
			}
		});

		if (shopOrder.getBillingsAddress() == null)
			shopOrder.setBillingsAddress(new Address());

		billingAddressController.setAddress(shopOrder.getBillingsAddress());

		if (shopOrder.getBillingsAddress() == shopOrder.getShippingAddress())
			deliveryAddressCheckbox.setSelected(true);
		else if (shopOrder.getShippingAddress() != null)
			deliveryAddressController.setAddress(shopOrder.getShippingAddress());

		marketplaceLabel.setText(shopOrder.getMarketplaceString());

		if (shopOrder.getOrderTime() != null)
			orderDateInput.setValue(shopOrder.getOrderTime().toLocalDateTime().toLocalDate());
		else
			orderDateInput.setValue(LocalDate.now());

		if (shopOrder.getInvoice() != null && shopOrder.getInvoice().getInvoiceTime() != null)
			billingDateInput.setValue(shopOrder.getInvoice().getInvoiceTime().toLocalDateTime().toLocalDate());
		else
			billingDateInput.setDisable(true);
			

		currencyInput.setText(shopOrder.getCurrencyCode());

		shippingInput.setText(AppUtils.formatCurrencyAmount(shopOrder.getShippingCosts()));
		shippingInput.textProperty().addListener((observable, oldVal, newVal) -> updateLabels());

		vatInput.setText(String.format("%.2f", shopOrder.getVatPercent()));
		vatInput.textProperty().addListener((a, b, c) -> updateLabels());

		orderNumberInput.setText(shopOrder.getOrderNumber());
		
		updateLabels();
	}

	
	private void updateLabels() {		
		try {
			var shippingCosts = AppUtils.parseDouble(shippingInput.getText(), true);
			var vatFactor = AppUtils.parseDouble(vatInput.getText(), true) / 100;
			var itemsTotalAmount = shopOrder.getItemsTotalAmount();

			var grossAmount = 1d * itemsTotalAmount / 100 + shippingCosts;
			var netAmount = grossAmount / (1d + vatFactor);

			grossAmountLabel.setText(String.format("%.2f", grossAmount));
			netAmountLabel.setText(String.format("%.2f", netAmount));
		} catch (ParseException pe) {
			System.out.println(pe.getMessage());
		}

	}

	public void handleSaveButton(ActionEvent actionEvent) {
		
		if (stage != null) {
			stage.close();

			try {
				if (items.size() == 0)
					shopOrder.setItems(new ArrayList<>());
				else
					shopOrder.setItems(Arrays.asList((LineItem[]) items.toArray(new LineItem[] {})));

				billingAddressController.saveToAddress(shopOrder.getBillingsAddress());

				if (!deliveryAddressCheckbox.isSelected()) {
					if (shopOrder.getShippingAddress() == null)
						shopOrder.setShippingAddress(new Address());
					deliveryAddressController.saveToAddress(shopOrder.getShippingAddress());
				} else {
					//shopOrder.setShippingAddress(null);
					shopOrder.setShippingAddress(shopOrder.getBillingsAddress());
				}

				shopOrder.setCurrencyCode(currencyInput.getText());
				shopOrder.setShippingCosts(Math.round(AppUtils.parseDouble(shippingInput.getText(),true) * 100));
				shopOrder.setVatPercent(AppUtils.parseDouble(vatInput.getText(),true));
				shopOrder.setTotalGrossAmount(shopOrder.getItemsTotalAmount() + shopOrder.getShippingCosts());
				shopOrder.setOrderNumber(orderNumberInput.getText());						
				
				
				var invoiceTime = billingDateInput.getValue();
				if(invoiceTime != null && shopOrder.getInvoice() != null) {				
					shopOrder.getInvoice().setInvoiceTime(Timestamp.valueOf(billingDateInput.getValue().atStartOfDay()));
				}
				
				shopOrder.setOrderTime(Timestamp.valueOf(orderDateInput.getValue().atStartOfDay()));
				
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		DataAccessBase.insertOrUpdate(shopOrder);
	}

	private void editCommit(CellEditEvent<LineItem, ?> e) {
		
		var rowIndex = e.getTablePosition().getRow();
		var item = items.get(rowIndex);
		var colId = e.getTableColumn().getId();

		switch (colId) {
		case "itemDescriptionColumn":
			item.getProduct().setTitle((String) e.getNewValue());
			break;
		case "itemTotalPriceColumn":
			item.setTotalPriceGross(Math.round(100d * (double) e.getNewValue()));
			break;
		case "itemPricePerItemColumn":
			item.setTotalPriceGross(Math.round(((double) e.getNewValue()) * item.getQuantity() * 100));
			break;
		case "itemQuantityColumn":
			var singleItemAmount = 1d * item.getTotalPriceGross() / ((long) e.getOldValue());
			item.setQuantity((long) e.getNewValue());
			item.setTotalPriceGross(Math.round(singleItemAmount * item.getQuantity()));
			break;
		}

		items.set(rowIndex, item);

		updateLabels();
	}

	public void handleAddOrderItem(ActionEvent e) {
		
		var item = new LineItem();
		item.setTotalPriceGross(Math.round(100d * 9.99d));
		item.setQuantity(1);
		var p = new Product(Math.round(item.getTotalPriceGross() / item.getQuantity()),
				"Testprodukt", 
				"" + (new Random().nextInt()),
				item.getOwner() == null || item.getOwner().getDataSource() == null ? null
						: item.getOwner().getDataSource().getToken().getOwner().platform);

		item.setProduct(p);
		item.setOwner(shopOrder);
		items.add(item);
		
		updateLabels();
	}

	public void handleRemoveOrderItem(ActionEvent e) {
		
		var index = orderItemsTable.getSelectionModel().getSelectedIndex();

		if (index != -1)
			items.remove(index);

		updateLabels();
	}
}
