package com.salesinvoicetools.controllers;

import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.CheckComboBox;
import com.salesinvoicetools.shopapis.ShopApiBase.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApiUpdateController {
    @FXML
    Slider apiPastXDaysSlider;

    @FXML
    Label apiPastDaysLabel;

    @FXML
    Button apiUpdateButton;

    @FXML
    Label statusLabel;

    @FXML
    ProgressBar progressBar;

    @FXML
    Button testButton;

    @FXML
    CheckComboBox<Marketplace> apiPlatformSelect;

    public DoubleProperty currentProgress;

    private Marketplace currentMarketplaceProcessing = null;


    public void initialize() {
        setUpApiContols();
        updateStatusLabel();
    }

    public List<Marketplace> getSelectedMarketPlaces() {
        return apiPlatformSelect.getCheckModel().getCheckedItems();
    }

    private void setUpApiContols() {

        testButton.setOnAction(actionEvent -> {
           getSelectedTokens().forEach(oAuth2Token -> {
               var api = ShopApiBase.getTargetShopApi(oAuth2Token);
               var order = api.getProduct("203633267889");

               AppUtils.log(order == null ? "Product is null" : order.toString());
           });
        });

        apiPastDaysLabel.setUserData(apiPastDaysLabel.getText());

        apiPastXDaysSlider.valueProperty().addListener((a,o,newVal) -> {
            apiPastDaysLabel.setText(((String)apiPastDaysLabel.getUserData()).replace("{x}", ""+newVal.intValue()));

            if(newVal.doubleValue() - Math.floor(newVal.doubleValue()) < .1) {
                return;
            }
            apiPastXDaysSlider.setValue(newVal.intValue());
        });
        AppUtils.persistChangesDouble(apiPastXDaysSlider.valueProperty(), "apiUpdateNumberPastDays", 5d);

        var apis = DataAccessBase.getAll(ApiAccess.class);
        var mps = apis.stream().map(a -> a.platform).collect(Collectors.toList());
        apiPlatformSelect.getItems().addAll(mps);
        apiPlatformSelect.setShowCheckedCount(true);

        AppUtils.persistChanges(apiPlatformSelect.getCheckModel(), "apiUpdatePlatformSelect");
        this.currentProgress = progressBar.progressProperty();
    }

    private List<OAuth2Token> getSelectedTokens() {
        var selectedMps = apiPlatformSelect.getCheckModel().getCheckedItems();

        //var tokens = DataAccessBase.<OAuth2Token>getAll(OAuth2Token.class);
        var tokens = DataAccessBase
                .getAll(OAuth2Token.class).stream()
                .filter(t -> selectedMps
                        .contains(t.owner.platform)).collect(Collectors.toList());
        AppUtils.log("Starting API-Update for following marketplaces: "+
                String.join(",", selectedMps.stream()
                        .map(mp -> ""+mp).collect(Collectors.joining())));

        return tokens;
    }

    public void updateRemoteOrders() {

        var tokens = getSelectedTokens();

        tokens.stream().filter(t -> t.isActive()).forEach(t -> {

            currentMarketplaceProcessing = t.owner.platform;

            var shopApi = ShopApiBase.getTargetShopApi(t);
            shopApi.retrieveOrders( (int) apiPastXDaysSlider.getValue(), progressBar.progressProperty());
        });

        currentMarketplaceProcessing = null;

        Platform.runLater(() -> {
            apiUpdateButton.setDisable(false);
            updateStatusLabel();
        });
    }

    private void updateStatusLabel() {
        List<DataSource> updates = DataAccessBase.getAll(DataSource.class);
        updates.sort(Comparator.comparing(DataSource::getTime));

        if(currentMarketplaceProcessing == null) {
            statusLabel.setText("letzte API-Aktualisierung: "
                    + (updates.size() == 0 ? " - " : AppUtils.formatDateTime(updates.get(0).time)));
        }
        else {
            statusLabel.setText("Bestellungen werden aktualisiert ("+currentMarketplaceProcessing+")");
        }
    }

    public void handleApiUpdateButton() {
        if (DataAccessBase.count(OAuth2Token.class) == 0) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Es wurden noch keine API-Konten angelegt.");
            a.show();
        } else {
            progressBar.setVisible(true);
            progressBar.setProgress(0);

            apiUpdateButton.setDisable(true);
            Thread thread = new Thread(() -> {
                updateRemoteOrders();
            });
            thread.start();
        }
    }

}
