package com.salesinvoicetools;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;


import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.controllers.AppSettingsController;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import com.salesinvoicetools.models.Address;
import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.AppConfiguration;
import com.salesinvoicetools.models.BankInfo;
import com.salesinvoicetools.models.ContactInfo;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.LineItem;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.OrderInvoice;
import com.salesinvoicetools.models.Product;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.models.ShopOrder.Marketplace;

public class AppWindow extends Application {

	public static Stage stage;
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		AppWindow.stage = primaryStage;
		primaryStage.getIcons().add(new Image("file:icon.png"));
		Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		    @Override
		    public void handle(WindowEvent t) {
		        Platform.exit();
		        System.exit(0);
		    }
		});
		
		Scene scene = new Scene(root, 1000, 770);
		scene.getStylesheets().add(AppWindow.class.getResource("style.css").toExternalForm());

		primaryStage.setTitle("Rechnungstool");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void subMain(String[] args) {
		
		if(DataAccessBase.count(AppConfiguration.class) == 0) {
			DataAccessBase.insertOrUpdate(new AppConfiguration());
		}
		
		launch(args);		
	}
}
