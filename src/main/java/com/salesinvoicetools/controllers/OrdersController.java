package com.salesinvoicetools.controllers;

import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.DataUpdateDataAccess;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.dataaccess.models.ShopOrdersFilterModel;
import com.salesinvoicetools.models.Address;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.shopapis.EbayShopApi;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import com.salesinvoicetools.viewmodels.ShopOrdersTableRow;
import com.salesinvoicetools.viewmodels.TokenSelectModel;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrdersController {

	@FXML
	javafx.scene.control.Pagination ordersPagination;

	@FXML
	AnchorPane tableAnchorPane;

	@FXML
	BorderPane pageBorderPane;

	@FXML
	Label ordersSelectedLabel;

	@FXML
	ChoiceBox<TokenSelectModel> marketplaceFilterSelect;

	@FXML
	TableColumn<ShopOrdersTableRow, String> orderNumberCol;

	@FXML
	TableColumn<ShopOrdersTableRow, String> dateCol;

	@FXML
	TableColumn<ShopOrdersTableRow, String> amountCol;

	@FXML
	TableColumn<ShopOrdersTableRow, String> sourceColumn;

	@FXML
	TableColumn<ShopOrdersTableRow, String> invoiceNrCol;

	@FXML
	TableColumn<ShopOrdersTableRow, String> buyerCol;

	@FXML
	TableColumn<ShopOrdersTableRow, String> statusCol;

	@FXML
	TableColumn<ShopOrdersTableRow, Void> actionCol;

	@FXML
	public TableColumn<ShopOrdersTableRow, Boolean> selectionCol;

	@FXML
	private TableView<ShopOrdersTableRow> ordersTable;

	@FXML
	Label statusLabel;

	@FXML
	ProgressIndicator statusLoadingIndicator;

	@FXML
	Button apiUpdateButton;

	@FXML
	ProgressBar progressBar;

	@FXML
	TextField searchInput;

	@FXML
	Spinner<Integer> entriesPerPageInput;

	@FXML
	Pane filterContainer;

	private long totalEntries;

	private ObservableList<ShopOrdersTableRow> currentPageItems;

	private HashSet<Long> selectedOrderIds = new HashSet<>();

	private ShopOrdersFilterModel filter = new ShopOrdersFilterModel();

	public void initialize() {

		if(orderNumberCol == null)
			return;
		
		try {

			orderNumberCol.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().order.getOrderNumber()));
			orderNumberCol.setUserData("orderNumber");

			dateCol.setCellValueFactory(
					item -> new SimpleStringProperty(AppUtils.formatDateTime(item.getValue().order.getOrderTime())));
			dateCol.setUserData("orderTime");

			amountCol.setCellValueFactory(item -> new SimpleStringProperty(
					AppUtils.formatCurrencyAmount(item.getValue().order.getTotalGrossAmount())));
			amountCol.setUserData("totalGrossAmount");

			sourceColumn.setCellValueFactory(item -> {
				var order = item.getValue().order;
				var text = order.getMarketplaceString();

				if (order.getDataSource() != null) {
					text = order.getDataSource().getToken().getName() + "\r\n" + text + "";
				}
				return new SimpleStringProperty(text);
			});
			sourceColumn.setUserData("platform");

			invoiceNrCol.setCellValueFactory(item -> new SimpleStringProperty(
					item.getValue().order.getInvoice() == null ? "" : "" + item.getValue().order.getInvoice().getInvoiceNumber()));

			buyerCol.setCellValueFactory(item -> {
				var buyer = item.getValue().order.getBuyer();
				var addr = item.getValue().order.getBillingsAddress();
				var str = "";
				if (addr != null)
					str += addr.getName() + "\r\n";
				if (buyer != null)
					str += buyer.getUserName();

				return new SimpleStringProperty(str);

			});

			selectionCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectionCol));
			var checkbox = new CheckBox();
			checkbox.selectedProperty().addListener((obs, old, newVal) -> {
				var ids = currentPageItems.stream().map(i -> i.order.getId()).collect(Collectors.toList());

				if (newVal)
					selectedOrderIds.addAll(ids);
				else
					selectedOrderIds.removeAll(ids);
				updateOrdersView();				
			});

			var checkboxContainer = new VBox(checkbox);
			checkboxContainer.setAlignment(Pos.CENTER);
			selectionCol.setGraphic(checkboxContainer);
			selectionCol.setCellValueFactory(cell -> cell.getValue().selectedProperty);
			selectionCol.setEditable(true);
			
			statusCol.setCellValueFactory(cell -> {
				var o = cell.getValue().order;
				var str = new ArrayList<String>();
				if(o.getPaymentComplete() != null && o.getPaymentComplete())
					str.add("bezahlt");
				if(o.getShippedTime() != null)
					str.add("verschickt");
				
				return new SimpleStringProperty(String.join(",", str));
			});

			actionCol.setCellFactory(
					new Callback<TableColumn<ShopOrdersTableRow, Void>, TableCell<ShopOrdersTableRow, Void>>() {

						@Override
						public TableCell<ShopOrdersTableRow, Void> call(
								final TableColumn<ShopOrdersTableRow, Void> param) {

							return new TableCell<ShopOrdersTableRow, Void>() {
								private final Button removeButton = new Button();
								private final Button editButton = new Button();

								private ShopOrder getData() {
									return getTableView().getItems().get(getIndex()).order;
								}

								private final Button invoiceButton = new Button();
								{
									editButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT, "16px"));
									editButton.setOnAction((ActionEvent e) -> {
										showOrderDetails(getData());
									});

									removeButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.REMOVE, "16px"));
									removeButton.setOnAction((ActionEvent e) -> {
										removeOrder(getData());
									});

									invoiceButton
											.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILE_PDF_ALT, "16px"));
									invoiceButton.setOnAction((ActionEvent e) -> {
										AppUtils.generateOrderInvoices(new Long[] { getData().getId() });
										updateOrdersView();
									});
								}

								@Override
								public void updateItem(Void item, boolean empty) {
									super.updateItem(item, empty);
									if (empty) {
										setGraphic(null);
									} else {
										var hbox = new HBox(editButton, removeButton, invoiceButton);
										hbox.setSpacing(5);
										setGraphic(hbox);
									}
								}
							};
						}
					});

			ordersPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
				filter.page = newIndex.intValue();
				updateOrdersView();
			});

			var allTokens = DataAccessBase.getAll(OAuth2Token.class).toArray(new OAuth2Token[0]);
			var tokenSelectOptions = Stream.concat(
					Arrays.<OAuth2Token>stream(allTokens)
							.map(t -> new TokenSelectModel(t, t.getName() + " | " + t.getOwner().getPlatform(), false)),
					Arrays.stream(
							new TokenSelectModel[] { new TokenSelectModel(null, "( ohne / manuell angelegt )", false),
									new TokenSelectModel(null, "( alle )", true) }))
					.toArray(TokenSelectModel[]::new);
			marketplaceFilterSelect.setItems(FXCollections.observableArrayList(tokenSelectOptions));
			marketplaceFilterSelect.getSelectionModel().select(tokenSelectOptions.length - 1);

			entriesPerPageInput.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 500, 20));

			applyFilterValues();
			updateStatusLabel();
			updateSelectionInfoLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void applyFilterValues() {
		filter.searchText = ((TextField) pageBorderPane.lookup("#searchInput")).getText();
		filter.filterByToken = marketplaceFilterSelect.getValue();
		filter.entriesPerPage = entriesPerPageInput.getValue().intValue();
		filter.onlyWithoutInvoice = ((CheckBox) pageBorderPane.lookup("#withoutInvoiceInput")).isSelected();
		filter.orderStartDate = ((DatePicker) pageBorderPane.lookup("#filterStartDateInput")).getValue();
		updateOrdersView();
	}

	private void updateStatusLabel() {
		List<DataSource> updates = DataAccessBase.getAll(DataSource.class);
		updates.sort((a, b) -> a.getTime().compareTo(b.getTime()));

		statusLabel.setText("letzte API-Aktualisierung: "
				+ (updates.size() == 0 ? " - " : AppUtils.formatDateTime(updates.get(0).getTime())));
	}

	private void updateOrdersView() {
		totalEntries = OrderDataAccess.count(filter);
		
		if(ordersPagination == null)
			return;
		
		ordersPagination
				.setPageCount(totalEntries % filter.entriesPerPage == 0 ? (int) (totalEntries / filter.entriesPerPage)
						: (int) (totalEntries / filter.entriesPerPage) + 1);

		List<ShopOrdersTableRow> orders = OrderDataAccess.getPage(filter).stream().map(order -> {

			var row = new ShopOrdersTableRow(order, selectedOrderIds.contains(order.getId()));
			row.selectedProperty.addListener((v, o, n) -> {
				if (n)
					selectedOrderIds.add(order.getId());
				else
					selectedOrderIds.remove(order.getId());
				updateSelectionInfoLabel();
			});
			return row;
		}).collect(Collectors.toList());

		if (currentPageItems == null) {
			currentPageItems = FXCollections.observableArrayList(orders);
			ordersTable.setItems(currentPageItems);
		} else {
			currentPageItems.clear();
			currentPageItems.addAll(orders);
		}

		updateSelectionInfoLabel();
	}

	private void updateSelectionInfoLabel() {
		long totalCount = DataAccessBase.count(ShopOrder.class);
		var text = selectedOrderIds.size() + " Bestellungen ausgewählt / " + totalEntries + " gefiltert / " + totalCount
				+ " insgesamt";
		ordersSelectedLabel.setText(text);
	}

	public void setStatusText(String text) {
		statusLabel.setText(text);
	}

	public void setLoading(boolean b) {
		statusLoadingIndicator.setVisible(b);
	}

	private void showOrderDetails(ShopOrder order) {
		try {
			Stage stage = new Stage();
			stage.setTitle("My modal window");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(((Node) pageBorderPane).getScene().getWindow());
			Parent root;
			var controller = new OrderDetailsController(order, stage);
			var loader = new FXMLLoader(AppWindow.class.getResource("AddUpdateOrder.fxml"));

			loader.setController(controller);
			root = loader.load();
			root.setId("orderDetails");
			root.getStylesheets().add("view/css/style.css");
			stage.setScene(new Scene(root));
			stage.showAndWait();

			updateOrdersView();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void selectNone() {
		currentPageItems.forEach(val -> {
			val.selectedProperty.set(false);
		});
		selectedOrderIds.clear();
		updateSelectionInfoLabel();
	}

	public void handleApiUpdateButton() {
		if (DataAccessBase.count(OAuth2Token.class) == 0) {
			Alert a = new Alert(AlertType.ERROR, "Es wurden noch keine API-Konten angelegt.");
			a.show();
		} else {
			progressBar.setVisible(true);
			progressBar.setProgress(0);

			apiUpdateButton.setDisable(true);
			Thread thread = new Thread(() -> {
				updateRemoteOrders(true);
			});
			thread.start();
		}
	}

	public void handleGeneratePdfButton() {
		AppUtils.generateOrderInvoices(selectedOrderIds.toArray(Long[]::new));
	}

	public void handleRemoveSelectedOrders() {
		var orders = selectedOrderIds.stream().map(i -> (ShopOrder) DataAccessBase.find(ShopOrder.class, i))
				.collect(Collectors.toList());
		orders.forEach(order -> removeOrder(order));

		updateOrdersView();
	}

	public void removeOrder(ShopOrder order) {
		order.setInvoice(null);
		DataAccessBase.insertOrUpdate(order);
		DataAccessBase.delete(order);
		selectedOrderIds.remove(order.getId());
		updateOrdersView();
	}

	public void handleAddOrderButton(ActionEvent e) {
		Address a = new Address();
		a.setName("Max Mustermann");
		a.setCity("Hamburg");
		var order = new ShopOrder();
		order.setBillingsAddress(a);
		showOrderDetails(order);
	}

	public void handleTestButtonAction() {
		var tokens = DataAccessBase.<OAuth2Token>getAll(OAuth2Token.class);
		tokens.stream().filter(t -> t.isActive()).forEach(t -> {
			EbayShopApi shopApi = (EbayShopApi)beforeApiCall(t);

			try {
				shopApi.insertTestListing(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});	
	}
	
	
	private ShopApiBase beforeApiCall(OAuth2Token t) {
		ShopApiBase shopApi = ShopApiBase.getTargetShopApi(t);

		if (shopApi.isTokenExpired()) {
			try {
				shopApi.refreshToken();
			} catch (IOException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		return shopApi;
	}

	public void updateRemoteOrders(boolean skipExisting) {
		var tokens = DataAccessBase.<OAuth2Token>getAll(OAuth2Token.class);
		tokens.stream().filter(t -> t.isActive()).forEach(t -> {

			ShopApiBase shopApi = beforeApiCall(t);
			
			var updates = DataUpdateDataAccess.getUpdatesForToken(t);
			Calendar timeFrom = Calendar.getInstance();
			var update = new DataSource(t, 0);

			if (updates.size() == 0) {
				timeFrom.add(Calendar.DAY_OF_YEAR, -30);
			} else {
				timeFrom.setTime(updates.get(0).getTime());
				//timeFrom.add(Calendar.DAY_OF_YEAR, -30);
			}

			update.setTime(Timestamp.from(Instant.now()));
			final var upd = update;

			var pageNumber = 1;
			var ordersProcessed = 0;

			try {				
				SimpleIntegerProperty remainingEntries = new SimpleIntegerProperty(0);

				do {					
					var ordersPage = shopApi.getOrdersPage(timeFrom, pageNumber++, 3, remainingEntries);
					ordersPage.forEach(o -> {
						o.setDataSource(upd);						

						if (!skipExisting
								|| OrderDataAccess.getByOrderNumber(o.getOrderNumber(), t.getOwner().getPlatform()) == null) {
							DataAccessBase.insertOrUpdate(o);
						}
					});

					ordersProcessed += ordersPage.size();
					var totalOrders = remainingEntries.get() + ordersProcessed;
					final double progress = 1d * ordersProcessed / totalOrders;

					final String statusText = "Rufe " + t.getOwner().getPlatform() + "-Bestellungen ab für Token " + t.getName() + ": "
							+ ordersProcessed + " / " + totalOrders;
					
					Platform.runLater(() -> {						
						progressBar.setProgress(progress);
						statusLabel.setText(statusText);				
					});

				} while (remainingEntries.get() > 0);

				upd.setNewEntries(ordersProcessed);
				Platform.runLater(() -> {
					DataAccessBase.insertOrUpdate(upd);
					DataUpdateDataAccess.removeUnusedUpdates(t);
					updateOrdersView();
				});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		Platform.runLater(() -> {
			apiUpdateButton.setDisable(false);
			updateStatusLabel();
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void tableSortHandler(SortEvent<TableView<ShopOrdersTableRow>> event) {
		String newSortField = filter.sortField;
		SortType newSortType = filter.sortType;

		TableColumn col = null;
		if (ordersTable.getSortOrder().size() > 0) {
			col = ordersTable.getSortOrder().get(0);

			newSortField = (String) col.getUserData();
			newSortType = col.getSortType();
		}

		if (filter.sortField.equals(newSortField) && filter.sortType == newSortType)
			return;

		filter.sortField = newSortField;
		filter.sortType = newSortType;

		updateOrdersView();
		ordersTable.getSortOrder().add(col);
		event.consume();
	}

}
