package com.salesinvoicetools.controllers;

import com.google.api.client.util.Strings;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import com.salesinvoicetools.viewmodels.StockItemDetailsModel;
import com.salesinvoicetools.viewmodels.StockItemTableRow;
import com.salesinvoicetools.viewmodels.VariationPriceTableRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class StockItemsController {

    @FXML
    TableView<StockItemTableRow> stockItemsTable;

    @FXML
    TableColumn<StockItemTableRow, String> titleCol;

    @FXML
    TableColumn<StockItemTableRow, String> itemPriceCol;

    @FXML
    TableColumn<StockItemTableRow, String> imageCol;

    @FXML
    TableColumn<StockItemTableRow, Long> stockCol;

    @FXML
    TableColumn<StockItemTableRow, ShopApiBase.Marketplace[]> liveCol;

    @FXML
    TableColumn<VariationPriceTableRow, String> variationPriceCol;

    @FXML
    TextArea itemDescriptionField;

    @FXML
    TextField itemTitleField;

    @FXML
    VBox tokensSyncContrainer;

    @FXML
    VBox variationsContainer;

    @FXML
    Button addVariationButton;

    @FXML
    Button saveChangesButton;

    @FXML
    VBox itemDetailsContainer;

    @FXML
    VBox pricesContainer;

    @FXML
    TableView pricesTable;

    @FXML
    Button addImageButton;

    @FXML
    FlowPane imagesContainer;

    @FXML
    Label idLabel;

    //@FXML
    //StackPane imageTemplate;

    ObservableList<StockItemTableRow> currentPageItems;

    ObservableList<VariationPriceTableRow> currentPriceRows;


    private StockItemDetailsModel detailsModel = null;

    private StockItem getSelectedItem() {
        var item =  stockItemsTable.getSelectionModel().getSelectedItem();
        return item == null ? null : item.item;
    };



    public void initialize() {

        currentPageItems = FXCollections.observableList(new ArrayList<StockItemTableRow>());
        stockItemsTable.setItems(currentPageItems);

        currentPriceRows = FXCollections.observableList(new ArrayList<>());
        pricesTable.setItems(currentPriceRows);

        stockItemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateStockItemDetailsView(newSelection.item);
        });

        addImageButton.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            var f = fileChooser.showOpenDialog(addImageButton.getParent().getScene().getWindow());
            if(f != null && f.isFile() && f.exists()) {
                var id = getSelectedItem().id;
                var itemImagesFolder = new File(getSelectedItem().getLocalImagesPath());
                itemImagesFolder.mkdirs();

                var numFiles = Arrays.stream(itemImagesFolder.listFiles()).filter(file -> file.isFile()).count();
                try {
                    var file  = itemImagesFolder+"/"+numFiles+"."+AppUtils.getFileFormat(f.toString());
                    Files.copy(f.toPath(),Path.of(file));
                    updateStockItemDetailsView(getSelectedItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        addVariationButton.setOnAction(actionEvent -> {
            var vari = new StockItemVariation();
            detailsModel.variations.add(new StockItemDetailsModel().new VariatiationModel(vari));

            updateVariationControls();
        });

        titleCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().item.title));

        imageCol.setCellValueFactory(v -> {
            var images = v.getValue().item.getLocalImages();

            if(images != null && images.length > 0)
                return new SimpleStringProperty(images[0].toString());
            else
                return new SimpleStringProperty("");
        });


        imageCol.setCellFactory(param -> {

            var cell = new TableCell<StockItemTableRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    if(empty || Strings.isNullOrEmpty(item)) {
                        setGraphic(null);
                    }
                    else
                    {
                        try {
                            AppUtils.cacheImage(item, AppUtils.ImageSize.SM);
                            var img = AppUtils.readImageFile(item);
                            //ImageView imageView = new ImageView(AppUtils.convertWritableImage(img));
                            //ImageView imageView = new ImageView(AppUtils.convertWritableImage(img));
                            ImageView imageView = new ImageView("file:"+item);
                            setGraphic(imageView);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            return cell;
        });

        variationPriceCol.setCellFactory(col -> {
            var cell = new TableCell<VariationPriceTableRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    if(empty) {
                        setGraphic(null);
                    }
                    else {
                        var field = new TextField(item);
                        field.textProperty().addListener((observableValue, s, t1) -> {
                            try {
                                if(StringUtils.isNumeric(t1))
                                    getTableRow().getItem().price = AppUtils.parseCurrency(t1);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        });
                        setGraphic(field);
                    }
                }
            };

            return cell;
        });

        variationPriceCol.setCellValueFactory(v ->
        {
            var p = v.getValue().price == null ? null :AppUtils.formatDouble(v.getValue().price/100d);
            return new SimpleStringProperty(p);

        });

        updateStockItemsTable();

        saveChangesButton.setOnAction(actionEvent -> saveChanges());

    }

    private void updateStockItemDetailsView(StockItem newSelection){

        if(newSelection == null) {
            itemDetailsContainer.setVisible(false);
            detailsModel = null;
            return;
        }
        detailsModel = new StockItemDetailsModel(newSelection);
        idLabel.setText(""+newSelection.id);
        itemDetailsContainer.setVisible(true);
        var item = newSelection;
        //itemTitleField.setText(item.title);
        itemTitleField.textProperty().bindBidirectional(detailsModel.title);
        itemDescriptionField.textProperty().bindBidirectional(detailsModel.description);

        tokensSyncContrainer.getChildren().clear();
        var tokens = DataAccessBase.getAll(OAuth2Token.class);
        tokens.forEach(t -> {
            var cb = new CheckBox();
            cb.setText("["+t.owner.platform+"] "+t.name);
            tokensSyncContrainer.getChildren().add(cb);
        });

        imagesContainer.getChildren().clear();

        if(newSelection.getLocalImages() != null) {
            Arrays.stream(newSelection.getLocalImages()).forEach(f -> {
                if(!f.exists())
                    return;
                try {
                    addGalleryImageView(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        updateVariationControls();
        updatePricesTable();
    }

    private void addGalleryImageView(File f) throws IOException {
        var imgContainer = new StackPane();
        imgContainer.prefWidth(80);
        imgContainer.prefHeight(80);

        var b = new Button("X");
        b.setOnAction(actionEvent -> {
            try {
                AppUtils.deleteImageFromCache(f.toString(), AppUtils.ImageSize.SM);
                f.delete();

            } catch (IOException e) {
                e.printStackTrace();
            }
            updateStockItemDetailsView(getSelectedItem());
            stockItemsTable.refresh();
        });

        var imgView = new ImageView();
        var imgCache = AppUtils.getCacheImageLocation(f.toString(), AppUtils.ImageSize.SM);
        AppUtils.log(imgCache);
        imgView.setImage(new Image("file:"+imgCache));
        imgView.setFitWidth(80);
        imgView.setFitHeight(80);

        imgContainer.getChildren().addAll(imgView,b);
        imgContainer.setAlignment(b, Pos.BOTTOM_RIGHT);
        imagesContainer.getChildren().add(imgContainer);
    }

    private void saveChanges() {
        if(detailsModel == null)
            return;

        detailsModel.item.title = detailsModel.title.getValue();
        detailsModel.item.description = detailsModel.description.getValue();

        var attributes = detailsModel.item.attributes;


        detailsModel.variations.forEach(variationModel -> {
            var variation = variationModel.variation;

            if(!detailsModel.item.variations.contains(variation))
                detailsModel.item.variations.add(variation);

            variation.owner = detailsModel.item;


            if(variation.title != null)
            if(!variation.title.equals(variationModel.name.get())) {
                detailsModel.item.attributes.forEach(a -> {
                    a.targetVariation = a.targetVariation.replace(
                            "\""+variation.title+"\":",
                            "\""+variationModel.name.get()+"\":");
                });
            }

            variation.title = variationModel.name.get();
            variation.possibleValues = variationModel.possibleValues.get();
            variation.affectsStock = variationModel.affectsStock.get();
            variation.affectsPrice = variationModel.affectsPrice.get();

            if(!variation.affectsPrice) {
                detailsModel.item.attributes.forEach(a -> {

                });
            }

        });

        DataAccessBase.insertOrUpdate(detailsModel.item);
        stockItemsTable.refresh();

        //updateStockItemsTable();
        updatePricesTable();
    }

    private void updateStockItemsTable() {
        var items = DataAccessBase.getAll(StockItem.class)
                .stream().map(i -> new StockItemTableRow(i, false)).collect(Collectors.toList());

        currentPageItems.clear();
        currentPageItems.addAll(items);

    }

    public void handleSavePrices() {
        currentPriceRows.forEach(row -> {
            AppUtils.log("saving price "+row.price);
            getSelectedItem().setAttribute(row.variationValues, "price", ""+row.price);

        });
        updatePricesTable();
  }

    public void updatePricesTable() {
        pricesTable.getColumns().removeIf(o -> {
            var col = (TableColumn) o;
            return col.getId() == null || !col.getId().equals("variationPriceCol");
        });
        currentPriceRows.clear();

        var variations = detailsModel.item.variations
                .stream().filter(v -> v.affectsPrice).collect(Collectors.toList());


        variations.forEach(v -> {
            var column = new TableColumn<VariationPriceTableRow, String>();
            column.setText(v.title);
            column.setCellValueFactory(f -> {
                var row = f.getValue();
                return new SimpleStringProperty(row.variationValues.get(v.title));
            });

            pricesTable.getColumns().add(column);
        });

        var firstVariation = variations.size() > 0 ? variations.get(0) : null;
        var firstVariationVals = firstVariation == null ? null : firstVariation.getPossibleValuesList();

        var secondVariation = variations.size() > 1 ? variations.get(1) : null;
        var secondVariationVals = secondVariation == null ? null : secondVariation.getPossibleValuesList();

        var thirdVariation = variations.size()> 2 ? variations.get(2) : null;
        var thirdVariationVals = thirdVariation == null ? null : thirdVariation.getPossibleValuesList();

        for(int i = 0; firstVariation != null && i < firstVariationVals.size(); i++) {
            var map = new HashMap<String, String>();
            map.put(firstVariation.title, firstVariationVals.get(i));

            if(secondVariation != null) {
                for(int h = 0;secondVariation != null &&  h < secondVariationVals.size(); h++) {
                    map.put(secondVariation.title, secondVariationVals.get(h));

                    if(thirdVariation != null) {

                        for(int j = 0;thirdVariation != null &&  j < thirdVariationVals.size(); j++) {
                            map.put(thirdVariation.title, thirdVariationVals.get(h));

                            var map3 = new HashMap<>(map);
                            currentPriceRows.add(new VariationPriceTableRow(map3, detailsModel.item.getPrice(map3) ));
                        }
                    }

                    var map2 = new HashMap<>(map);
                    currentPriceRows.add(new VariationPriceTableRow(map2,  detailsModel.item.getPrice(map2)));
                }
            }
            else {
                currentPriceRows.add(new VariationPriceTableRow(map,  detailsModel.item.getPrice(map)));
            }
        }
    }

    private void updateVariationControls() {
        //var i = getSelectedItem();
        variationsContainer.getChildren().clear();

         detailsModel.variations.forEach(v -> {
             var tf = new TextField();
             tf.setPromptText("Farbe");
             var priceCB = new CheckBox("Beeinflusst Preis");
             var stockCB = new CheckBox("Beeinflusst Lagerbest.");
             var valuesInput = new TextField();
             valuesInput.setPromptText("Grün, Braun, Transparent");
             priceCB.selectedProperty().bindBidirectional(v.affectsPrice);
             stockCB.selectedProperty().bindBidirectional(v.affectsStock);
             tf.textProperty().bindBidirectional(v.name);
             valuesInput.textProperty().bindBidirectional(v.possibleValues);

             var variationContainer  = new VBox();
             variationContainer.setSpacing(5);

             variationContainer.getChildren().addAll(tf,valuesInput,priceCB,stockCB, new Separator());
             variationsContainer.getChildren().add(variationContainer);

         });
    }


    public void addItemClick(){

        var item = new StockItem();
        item.title = "asdf";

        DataAccessBase.insertOrUpdate(item);
        updateStockItemsTable();
    }


}
