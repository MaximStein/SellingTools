package com.salesinvoicetools.controllers;

import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.ApiAccessDataAccess;
import com.salesinvoicetools.dataaccess.AppSettings;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.shopapis.ShopApiBase.*;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.utils.AppUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ApiAccessController {



	@FXML
	Button saveApiAccessButton;

	@FXML
	TextField clientIdTextField;

	@FXML
	TextField clientSecretTextField;
	
	@FXML
	TextField callbackUrlTextField;

	@FXML
	TreeView<Object> apiAccessTreeView;

	@FXML
	Button addAccountButton;

	@FXML
	Button addApiButton;

	@FXML
	ChoiceBox<Marketplace> platformChoiceBox;


	@FXML
	CheckBox apiEntryActiveCheckbox;

	@FXML
	TextField firstEditingField;

	@FXML
	TextField secondEditingField;

	@FXML
	TextField editingField3;

	@FXML
	Button saveButton;

	@FXML
	ColorPicker tokenColorPicker;


	public void initialize() {

		if (addApiButton != null)
			addApiButton.setDisable(getUnusedApis().size() == 0);

		if (platformChoiceBox != null) {
			platformChoiceBox.setItems(FXCollections.observableArrayList(getUnusedApis()));
			platformChoiceBox.getSelectionModel().select(0);
		}

		if (apiAccessTreeView != null) {			
			apiAccessTreeView.getSelectionModel().selectedItemProperty()
					.addListener((observable, oldVal, newVal) -> {

						addAccountButton.setDisable(true);
						apiEntryActiveCheckbox.setSelected(false);
						apiEntryActiveCheckbox.setDisable(true);
						firstEditingField.setDisable(true);
						firstEditingField.clear();
						secondEditingField.clear();
						secondEditingField.setDisable(true);
						editingField3.setDisable(true);
						editingField3.clear();



						if (newVal != null) {
							if (newVal.getValue() instanceof ApiAccess) {
								var item = (ApiAccess) newVal.getValue();
								addAccountButton.setDisable(false);
								firstEditingField.setText(item.clientId);
								firstEditingField.setDisable(false);

								secondEditingField.setText(item.clientSecret);
								secondEditingField.setDisable(false);

								editingField3.setText(item.callbackUrl);
								editingField3.setDisable(false);

								tokenColorPicker.setDisable(true);
							} else if (newVal.getValue() instanceof OAuth2Token) {
								var item = (OAuth2Token) newVal.getValue();

								apiEntryActiveCheckbox.setSelected(item.isActive);
								apiEntryActiveCheckbox.setDisable(false);

								firstEditingField.setDisable(false);
								firstEditingField.setText(item.name);

								secondEditingField.setText(item.accessToken);

								tokenColorPicker.setDisable(false);
								var settingsKey = AppSettings.TOKEN_COLOR_+item.owner.platform+"_"+item.name;
								var clr = AppSettings.getString(settingsKey, "#EEEEEE");

								tokenColorPicker.setValue(Color.web(clr));
							}
						}
					});
			updateApiTreeView();
		}

		if(apiEntryActiveCheckbox != null) {
			apiEntryActiveCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov,
					Boolean old_val, Boolean new_val) -> {
						var selectedValue = apiAccessTreeView.getSelectionModel().getSelectedItem().getValue();
						if(selectedValue instanceof OAuth2Token) {
							((OAuth2Token)selectedValue).isActive = new_val;
							DataAccessBase.insertOrUpdate(selectedValue);
						}
					});			
		}

		if(saveButton != null) {
			saveButton.setOnAction(actionEvent -> {
				AppUtils.log(getSelectedItem() == null ? " - ": getSelectedItem().toString());
				var item = getSelectedItem();

				if(item instanceof OAuth2Token ) {
					var i = (OAuth2Token)item;
					i.name = firstEditingField.getText();
					DataAccessBase.insertOrUpdate(item);

					var settingsKey = AppSettings.TOKEN_COLOR_+i.owner.platform+"_"+i.name;
					var clr = AppSettings.getString(settingsKey, "#EEEEEE");

					AppSettings.setString(settingsKey,AppUtils.toRGBCode(tokenColorPicker.getValue()));
				}
				else if(item instanceof ApiAccess ) {
					((ApiAccess)item).clientId = firstEditingField.getText();
					((ApiAccess)item).clientSecret = secondEditingField.getText();
					((ApiAccess)item).callbackUrl = editingField3.getText();



					DataAccessBase.insertOrUpdate(item);
				}
			});
		}

	}

	public Object getSelectedItem() {
		return apiAccessTreeView.getSelectionModel().getSelectedItems().size() == 0
				? null
				: apiAccessTreeView.getSelectionModel().getSelectedItem().getValue();
	}

	/**
	 * returns the enum-values of marketplaces that aren't assigned to any API-access 
	 * @return
	 */
	private List<Marketplace> getUnusedApis() {
		List<ApiAccess> apiAccesses = DataAccessBase.getAll(ApiAccess.class);

		Marketplace[] savedVals = apiAccesses.stream().map(item -> item.platform).toArray(Marketplace[]::new);
		var allVals = Arrays.asList(Marketplace.class.getEnumConstants());

		return allVals.stream().filter(item -> item != Marketplace.OTHER && !Arrays.asList(savedVals).contains(item)).collect(Collectors.toList());
	}

	/**
	 * displays the modal window for adding an api access (api key & secret)
	 * @param owner
	 */
	private void showAddApiModal(Window owner) {
		try {
			Stage stage = new Stage();
			stage.setTitle("+ API-Zugang");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(owner);
			Parent root;
			root = FXMLLoader.load(AppWindow.class.getResource("AddApiModal.fxml"));
			stage.setScene(new Scene(root, 500, 400));
			stage.showAndWait();
			updateApiTreeView();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void updateApiTreeView() {
		var apiAccessRootNode = new TreeItem<Object>("APIs");
		apiAccessRootNode.setExpanded(true);

		var apiAccessEntries = apiAccessRootNode.getChildren();
		List<ApiAccess> items = DataAccessBase.getAll(ApiAccess.class);
		items.stream().forEach(item -> {
			var treeItem = new TreeItem<Object>(item);
			item.tokens.stream().forEach(token -> {
				var subItem = new TreeItem<Object>(token);
				subItem.setExpanded(true);
				treeItem.getChildren().add(subItem);
			});
			apiAccessEntries.add(treeItem);
			treeItem.setExpanded(true);
		});
		apiAccessTreeView.setRoot(apiAccessRootNode);
	}
	
	@FXML
	protected void handleRemoveApiButton(ActionEvent e) {
		var item = apiAccessTreeView.getSelectionModel().getSelectedItem();
		if (item == null)
			return;		
		
		if(item.getValue() instanceof OAuth2Token) {
			var token = (OAuth2Token) item.getValue();
			token.owner.tokens.remove(token);
			ApiAccessDataAccess.deleteToken(token);
			DataAccessBase.insertOrUpdate(token.owner);
		}
		else if(item.getValue() instanceof ApiAccess)
			DataAccessBase.delete(item.getValue());
		
		updateApiTreeView();
	}

	/**
	 * handles the oauth2 authentication process for the selected API by showing a web view and saving 
	 * the oauth-token when the user logged in successfully  
	 * @param e
	 */
	@FXML
	protected void handleAddAccountButton(ActionEvent e) {
		
		var selectedApiNode = apiAccessTreeView.getSelectionModel().getSelectedItem();

		if (selectedApiNode == null || !(selectedApiNode.getValue() instanceof ApiAccess))
			return;
		
		var api = (ApiAccess) selectedApiNode.getValue();
		
		Optional<String> result;
		boolean showWebView = false;
		String headerMessage = "";
		
		do {		
			TextInputDialog dialog = new TextInputDialog("");
			dialog.setHeaderText(headerMessage);
			dialog.setContentText("Bezeichnung des Zugangs:");
			result = dialog.showAndWait();
			
			if(result.isEmpty())
				return;
			
			if(result.get().length() == 0)
			{
				headerMessage = "Bitte das Feld ausfüllen.";
				continue;
			}	
			else 
			{			
				var input = result.get();
				if(DataAccessBase.<OAuth2Token>getAll(OAuth2Token.class).stream().anyMatch(t -> t.name.equals(input)))
				{
					headerMessage = "Eintrag mit dieser Bezeichnung existiert bereits.";
					continue;
				}
			}
			showWebView = true;
		}
		while(!showWebView);
				
		OAuth2Token token = new OAuth2Token();
		token.owner = api;
		token.name = result.orElse("Neuer Token");

		ShopApiBase shopApi = ShopApiBase.getTargetShopApi(token);

		Stage stage = new Stage();
		WebView webView = new WebView();
		webView.setPrefSize(600, 600);
		stage.setScene(new Scene(webView, 600, 600));
		stage.setTitle("OAuth Authentifizierung");
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(((Node) e.getSource()).getScene().getWindow());
		var engine = webView.getEngine();
		
//		final String secretState = "secret" + new Random().nextInt(999_999);

		var authUrl = shopApi.getOAuth2AuthorizationUrl();
		System.out.println("AUTH-URL: "+authUrl);
		
		engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
				System.out.println(engine.getLocation());


				System.out.println("STATE SUCCEEDED");

				stage.setTitle(engine.getLocation());

				try {
					var code = AppUtils.getParameterVal(engine.getLocation(), "code");

					if (code == null) {
						System.out.println("code not found in URL");
						return;
					}

					if(shopApi.tradeAccessTokenForCode(URLDecoder.decode(code, "UTF-8"))) {
						token.owner = api;
						api.tokens.add(token);

						DataAccessBase.insertOrUpdate(api);
						AppUtils.showNotification("Zugang gespeichert",
								"API-Konto Nr. "+DataAccessBase.count(OAuth2Token.class)+" eingetragen",
								AppUtils.NotificationType.INFO
						);
						stage.close();
					}

					updateApiTreeView();

				} catch (IOException | InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		stage.show();
		webView.getEngine().load(authUrl);

	}

	@FXML
	protected void handleSaveApiButton(ActionEvent e) {
		var entry = new ApiAccess(platformChoiceBox.getValue(), clientIdTextField.getText(),
				clientSecretTextField.getText(), callbackUrlTextField.getText());
		((Stage) ((Node) e.getSource()).getScene().getWindow()).close();

		if (entry.clientSecret.length() > 0) {
			DataAccessBase.insertOrUpdate(entry);
		}
	}

	@FXML
	protected void handleAddApiButton(ActionEvent e) {
		showAddApiModal(((Node) e.getSource()).getScene().getWindow());
	}
}
