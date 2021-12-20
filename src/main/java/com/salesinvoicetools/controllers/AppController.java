package com.salesinvoicetools.controllers;

import com.salesinvoicetools.utils.AppUtils;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringUtils;

;import java.awt.datatransfer.StringSelection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * JavaFX-controller class for the root view
 * @author Maxim Stein
 *
 */
public class AppController {


	@FXML
	public BorderPane ordersOverviewPage;

	@FXML
	AnchorPane ordersPane;

	@FXML
	TabPane mainTabPane;

	@FXML
	AppSettingsController appSettingsPageController;

	@FXML
	WebView logWebView;

	public static AppController instance;

	private LinkedList<String> logContent = new LinkedList<>();

	public void initialize() {

		AppController.instance = this;
		AppUtils.log("App gestartet");

		mainTabPane.getSelectionModel().selectedItemProperty()
			.addListener((ObservableValue<? extends Tab> ov, Tab t, Tab t1) -> {
				if(t1.getId().equals("settingsTab")) {					
					appSettingsPageController.initialize();
				}
			});
	}

	private void updateWebView() {
		var css = "body { " +
				"background-color:#222222; " +
				"display: flex;" +
				"flex-direction: column-reverse;" +
				"font-family:Consolas;" +
				"color:#eee;" +
				" white-space:pre;" +
				"} " +
				".time,.tag{color:grey}";
		var content = "<html><head><style>"+css+"</style></head><body>";
		content += String.join("", logContent);
		content+="</body></html>";

		logWebView.getEngine().loadContent(content);
		logWebView.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight)");
	}


	public void log(String tag, String content) {

		log("silver",tag,content);
	}

	public void log(String color, String tag, String content) {

		Platform.runLater(() -> {

		var e = logWebView.getEngine();
		var t = AppUtils.formatTimeOfDay(Instant.now());

		if(logContent.size() > 1000) {
			logContent.removeFirst();
		}

		var paddedTag = StringUtils.leftPad(AppUtils.shortenString(tag,0, 28), 30, ".");

		logContent.add("<div><span class=time>["+t+"</span> <span class=tag>| "+paddedTag+"]</span>  " +
				"<span class=content style='color:"+color+"'>"+content+"</span></div>");


		updateWebView();

		});
	}

}
