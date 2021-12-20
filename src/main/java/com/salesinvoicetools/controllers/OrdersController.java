package com.salesinvoicetools.controllers;

import com.google.api.client.util.Strings;
import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.AppSettings;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.dataaccess.models.ShopOrdersFilterModel;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.ebay.EbayShopApi;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import com.salesinvoicetools.viewmodels.ShopOrdersTableRow;
import com.salesinvoicetools.viewmodels.TokenSelectModel;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.CheckComboBox;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrdersController {

	public static OrdersController instance;
	@FXML
	javafx.scene.control.Pagination ordersPagination;

	@FXML
	AnchorPane tableAnchorPane;

	@FXML
	BorderPane pageBorderPane;

	@FXML
	Label ordersSelectedLabel;

	@FXML
	CheckComboBox<TokenSelectModel> marketplaceFilterBox;

	@FXML
	public VBox mainContainer;

	@FXML
	TableColumn<ShopOrdersTableRow, String> orderNumberCol;

	@FXML
	TableColumn<ShopOrdersTableRow, Timestamp> dateCol;

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
	TableColumn<ShopOrdersTableRow, String> orderImageColumn;

	@FXML
	TableColumn<ShopOrdersTableRow, String> colorColumn;

	@FXML
	public TableColumn<ShopOrdersTableRow, Boolean> selectionCol;

	@FXML
	private TableView<ShopOrdersTableRow> ordersTable;

	@FXML
	private Button testButton;

	@FXML
	ProgressIndicator statusLoadingIndicator;



	@FXML
	ProgressBar progressBar;

	@FXML
	TextField searchInput;

	@FXML
	Spinner<Integer> entriesPerPageInput;

	@FXML
	Pane filterContainer;

	@FXML
	ApiUpdateController apiUpdateController;

	private long totalEntries;

	private ObservableList<ShopOrdersTableRow> currentPageItems;

	private HashSet<Long> selectedOrderIds = new HashSet<>();

	private ShopOrdersFilterModel filter = new ShopOrdersFilterModel();

	public void initialize() {
		OrdersController.instance = this;
		if(orderNumberCol == null)
			return;

		ordersTable.setRowFactory((table) -> {
			TableRow<ShopOrdersTableRow> row = new TableRow<>();
			return row;
		});

		try {
			orderNumberCol.setCellValueFactory(item ->
					new SimpleStringProperty(item.getValue().order.getOrderNumber()+"\r\n"
					+item.getValue().order.getItemsShortDescription(3)));
			orderNumberCol.setUserData("orderNumber");

			dateCol.setCellValueFactory(i -> new SimpleObjectProperty<>(i.getValue().order.getOrderTime()));

			dateCol.setCellFactory(col -> {
				TableCell<ShopOrdersTableRow, Timestamp> cell = new TableCell<ShopOrdersTableRow, Timestamp>() {
					@Override
					protected void updateItem(Timestamp item, boolean empty) {
						super.updateItem(item, empty);
						if(empty)
							setText("");
						else
							setText(AppUtils.formatDateTime(item));
					}
				};
				return cell;
			});

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

			colorColumn.setCellValueFactory(cell ->
			{
				var order = cell.getValue().order;
				var clr = AppSettings.getString(AppSettings.TOKEN_COLOR_+order.getMarketplace()+"_"+order.dataSource.token.name, "#EEE");
				return new SimpleStringProperty(clr);
			});

			colorColumn.setCellFactory(new Callback<>() {
				@Override
				public TableCell<ShopOrdersTableRow, String> call(
						final TableColumn<ShopOrdersTableRow, String> param) {

					var cell = new TableCell<ShopOrdersTableRow, String>() {
						@Override
						protected void updateItem(String item, boolean empty) {
							setStyle("-fx-background-color:" + item + ";");
						}
					};
					return cell;
				}
			});

			orderImageColumn.setCellValueFactory(cell -> {
				var order = cell.getValue().order;
				var images = order.getProductImages();

				return new SimpleStringProperty(images.length > 0 ? images[0] : null);
			});

			orderImageColumn.setCellFactory(new Callback<>() {
				@Override
				public TableCell<ShopOrdersTableRow, String> call(
						final TableColumn<ShopOrdersTableRow, String> param) {

					var cell = new TableCell<ShopOrdersTableRow, String>() {
						@Override
						protected void updateItem(String item, boolean empty) {
							if(empty || Strings.isNullOrEmpty(item)) {
								setGraphic(null);
							}
							else
							{
								try {
									var img = AppUtils.getImageFromCache(item, AppUtils.ImageSize.SM);
									ImageView imageView = new ImageView(AppUtils.convertWritableImage(img));
									setGraphic(imageView);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					};
					return cell;
				}
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
							.map(t -> new TokenSelectModel(t, t.getName() + " | " + t.getOwner().platform, false)),
					Arrays.stream(
							new TokenSelectModel[] { new TokenSelectModel(null, "( ohne / manuell angelegt )", false),
							//		new TokenSelectModel(null, "( alle )", true)
							}))
					.toArray(TokenSelectModel[]::new);
			marketplaceFilterBox.getItems().addAll(tokenSelectOptions);
			AppUtils.persistChanges(marketplaceFilterBox.getCheckModel(), "ordersOverviewFilter.tokenFilter");

			var intFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 500, 20);
			AppUtils.persistChangesInt(intFactory.valueProperty(), "ordersOverViewFilter.entriesPerPageInput", 20);
			entriesPerPageInput.setValueFactory(intFactory);


 			apiUpdateController.currentProgress.addListener((observableValue, number, t1) -> {
				Platform.runLater(() -> {
					updateOrdersView();
				});
			});

			applyFilterValues();
			updateSelectionInfoLabel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	public void applyFilterValues() {
		filter.searchText = ((TextField) pageBorderPane.lookup("#searchInput")).getText();
		filter.filterByToken = (marketplaceFilterBox.getCheckModel().getCheckedItems());
		filter.entriesPerPage = entriesPerPageInput.getValue().intValue();
		filter.onlyWithoutInvoice = ((CheckBox) pageBorderPane.lookup("#withoutInvoiceInput")).isSelected();
		filter.orderStartDate = ((DatePicker) pageBorderPane.lookup("#filterStartDateInput")).getValue();
		updateOrdersView();
	}



	private void updateOrdersView() {
		AppUtils.log("updating orders view");

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


	public void handleGenerateInvoicesButton() {
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
			EbayShopApi shopApi = (EbayShopApi)ShopApiBase.getTargetShopApi(t);

			try {
				shopApi.insertTestListing(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});	
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void tableSortHandler(SortEvent<TableView<ShopOrdersTableRow>> event) {

		TableColumn col = null;

		if (ordersTable.getSortOrder().size() > 0) {
			col = ordersTable.getSortOrder().get(0);

			filter.sortField = (String) col.getUserData();
			filter.sortType = col.getSortType();

			AppUtils.log("attempting to sort by "+filter.sortField+" ("+filter.sortType+")");
		}
		else {
			AppUtils.log("resetting sort order");
			filter.sortField = null;
			filter.sortType = null;
		}

		//if (filter.sortField.equals(newSortField) && filter.sortType == newSortType)
		//	return;


		//ordersTable.getSortOrder().add(col);
		//ordersTable.getSortOrder().clear();

		applyFilterValues();

	//	event.consume();
	}

}
