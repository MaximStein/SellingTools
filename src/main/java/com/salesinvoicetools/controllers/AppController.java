package com.salesinvoicetools.controllers;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

;


/**
 * JavaFX-controller class for the root view
 * @author Maxim Stein
 *
 */
public class AppController {

	@FXML
	BorderPane ordersOverviewPage;

	@FXML
	AnchorPane ordersPane;

	@FXML
	TabPane mainTabPane;

	@FXML
	AppSettingsController appSettingsPageController;
	
	public void initialize() {
		mainTabPane.getSelectionModel().selectedItemProperty()
			.addListener((ObservableValue<? extends Tab> ov, Tab t, Tab t1) -> {
				if(t1.getId().equals("settingsTab")) {					
					appSettingsPageController.initialize();
				}
				
			});
	}

}
