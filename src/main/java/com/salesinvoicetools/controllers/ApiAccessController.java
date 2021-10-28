package com.salesinvoicetools.controllers;

import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.ApiAccessDataAccess;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.ShopOrder.Marketplace;
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

	public void initialize() {

		if (addApiButton != null)
			addApiButton.setDisable(getUnusedApis().size() == 0);

		if (platformChoiceBox != null) {
			platformChoiceBox.setItems(FXCollections.observableArrayList(getUnusedApis()));
			platformChoiceBox.getSelectionModel().select(0);
		}

		if (apiAccessTreeView != null) {			
			apiAccessTreeView.getSelectionModel().selectedItemProperty()
					.addListener(new ChangeListener<TreeItem<Object>>() {
						@Override
						public void changed(ObservableValue<? extends TreeItem<Object>> observable,
								TreeItem<Object> oldVal, TreeItem<Object> newVal) {
							if (newVal != null && newVal.getValue() instanceof ApiAccess)
								addAccountButton.setDisable(false);
							else
								addAccountButton.setDisable(true);
							
							if(newVal != null && newVal.getValue() instanceof OAuth2Token) {
								apiEntryActiveCheckbox.setSelected(((OAuth2Token) newVal.getValue()).isActive());
								apiEntryActiveCheckbox.setDisable(false);
							}
							else {
								apiEntryActiveCheckbox.setSelected(true);								
								apiEntryActiveCheckbox.setDisable(true);
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
							((OAuth2Token)selectedValue).setActive(new_val);
							DataAccessBase.insertOrUpdate(selectedValue);
						}
					});			
		}
	}

	/**
	 * returns the enum-values of marketplaces that aren't assigned to any API-access 
	 * @return
	 */
	private List<Marketplace> getUnusedApis() {
		List<ApiAccess> apiAccesses = DataAccessBase.getAll(ApiAccess.class);

		Marketplace[] savedVals = apiAccesses.stream().map(item -> item.getPlatform()).toArray(Marketplace[]::new);
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
			stage.setTitle("API-Zugang hinzuf�gen");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(owner);
			Parent root;
			root = FXMLLoader.load(AppWindow.class.getResource("AddApiModal.fxml"));
			stage.setScene(new Scene(root, 400, 300));
			stage.showAndWait();
			updateApiTreeView();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void updateApiTreeView() {
		var apiAccessRootNode = new TreeItem<Object>("API Zug�nge");
		apiAccessRootNode.setExpanded(true);

		var apiAccessEntries = apiAccessRootNode.getChildren();
		List<ApiAccess> items = DataAccessBase.getAll(ApiAccess.class);
		items.stream().forEach(item -> {
			var treeItem = new TreeItem<Object>(item);
			item.getTokens().stream().forEach(token -> {
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
			token.getOwner().getTokens().remove(token);
			ApiAccessDataAccess.deleteToken(token);
			DataAccessBase.insertOrUpdate(token.getOwner());
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
				headerMessage = "Bitte das Feld ausf�llen.";
				continue;
			}	
			else 
			{			
				var input = result.get();
				if(DataAccessBase.<OAuth2Token>getAll(OAuth2Token.class).stream().anyMatch(t -> t.getName().equals(input)))
				{
					headerMessage = "Eintrag mit dieser Bezeichnung existiert bereits.";
					continue;
				}
			}
			showWebView = true;
		}
		while(!showWebView);
				
		OAuth2Token token = new OAuth2Token();
		token.setOwner(api);
		token.setName(result.orElse("Neuer Token"));
		api.getTokens().add(token);
					
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

				if (newState != State.SUCCEEDED)
					return;

				stage.setTitle(engine.getLocation());
				
				try {
					var code = AppUtils.getParameterVal(engine.getLocation(), "code");

					System.out.println(engine.getLocation());
					System.out.println(code);

					if (code == null)
						return;
					
					shopApi.tradeAccessTokenForCode(URLDecoder.decode(code, "UTF-8"));
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

		if (entry.getClientId().length() > 0 && entry.getClientId().length() > 0) {
			DataAccessBase.insertOrUpdate(entry);
		}
	}

	@FXML
	protected void handleAddApiButton(ActionEvent e) {
		showAddApiModal(((Node) e.getSource()).getScene().getWindow());
	}
}
