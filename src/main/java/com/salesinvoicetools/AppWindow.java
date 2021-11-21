package com.salesinvoicetools;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;


import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.controllers.AppSettingsController;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.shopapis.EtsyShopApi;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
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
import com.salesinvoicetools.shopapis.ShopApiBase.*;
import org.controlsfx.control.Notifications;
import org.h2.store.Data;

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
		
		Scene scene = new Scene(root, 1200, 900);
		scene.getStylesheets().add(AppWindow.class.getResource("style.css").toExternalForm());

		primaryStage.setTitle("Rechnungstool");
		primaryStage.setScene(scene);
		primaryStage.show();

		EtsyShopApi api = (EtsyShopApi) ShopApiBase.getTargetShopApi(DataAccessBase.getOneWhere(OAuth2Token.class, "name", "asdf"));

		/*try {
			if(api.refreshToken())
				System.out.println("tokenrefrehed yo");
			else
				System.out.println("couldnt refresh token");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} */

		//var res = api.getShopByUserId("242570917");
		//var res= api.getShopReceipts(AppUtils.intantToCalendar(Instant.now().minusSeconds(30*24*36000)),0, 10, true);

		var testProp = new SimpleIntegerProperty();
		List<ShopOrder> res = null;
		try {
			//res = api.getOrdersPage(AppUtils.intantToCalendar(Instant.now().minusSeconds(30*24*36000)), 1, 10, testProp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//var res= api.getShopReceipts("gravurshop");
	}

	public static void subMain(String[] args) {
		
		if(DataAccessBase.count(AppConfiguration.class) == 0) {
			DataAccessBase.insertOrUpdate(new AppConfiguration());
		}
		
		launch(args);		
	}
}
