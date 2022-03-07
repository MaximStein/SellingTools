package com.salesinvoicetools.controllers;


import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.AppSettings;
import static com.salesinvoicetools.dataaccess.AppSettings.*;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.Address;
import com.salesinvoicetools.models.BankInfo;
import com.salesinvoicetools.utils.AppUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.synedra.validatorfx.Check;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class AppSettingsController {


	@FXML
	VBox container;

	@FXML
	AddressController addressController;

	@FXML
	TextField kleinunternehmerTextInput;

	@FXML
	TextArea zusatztextInput;

	@FXML
	TextField ustIdInput;

	@FXML
	TextField bankNameInput;

	@FXML
	TextField ibanInput;

	@FXML
	TextField bicInput;

	@FXML
	CheckBox bankOnInvoiceCheckbox;

	@FXML
	TextField defaultTaxInput;

	@FXML
	CheckBox emailOnInvoiceCheckbox;
	
	@FXML
	TextField appDataDirectoryInput;

	@FXML 
	Spinner<Integer> invoiceNumberInput;

	@FXML
	CheckBox exportBusinessSettingsCheckbox;

	@FXML
	CheckBox exportApiDataCheckbox;

	@FXML
	Button exportDataButton;


	public void initialize() {

		defaultTaxInput.setTextFormatter(new TextFormatter<>(AppUtils.numberStringConverter));

		//settings = (AppConfiguration) DataAccessBase.getAll(AppConfiguration.class).get(0);

		addressController.setAddress(AppSettings.get(INVOICE_ADDRESS, Address.class));

		kleinunternehmerTextInput.setText(AppSettings.getString(KLEINUNTERNEHMER_INFO));

		zusatztextInput.setText(AppSettings.getString(INVOICE_ADDITIONAL_TEXT));

		ustIdInput.setText(AppSettings.getString(VAT_ID));

		var bankInfo = AppSettings.get(BANK_INFO, BankInfo.class);
		if(bankInfo != null) {
			bankNameInput.setText(bankInfo.bankName);
			ibanInput.setText(bankInfo.bankName);
			bicInput.setText(bankInfo.bic);
		}

		bankOnInvoiceCheckbox.setSelected(AppSettings.getBoolean(BANK_INFO_ON_INVOICE, false));

		emailOnInvoiceCheckbox.setSelected(AppSettings.getBoolean(EMAIL_ON_INVOICE, false));

		appDataDirectoryInput.setText(AppSettings.getString(AppSettings.APP_DATA_DIRECTORY));

		defaultTaxInput.setText(AppUtils.numberStringConverter.toString(AppSettings.getDouble(DEFAULT_TAX)));
		
		invoiceNumberInput.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
				1,
				java.lang.Integer.MAX_VALUE,
				AppSettings.getInt(CURRENT_INVOICE_NUMBER, 1)+1));

		exportDataButton.setOnAction(actionEvent -> handleDataExportButton());

	}

	public void handleSaveButton() {
		var addr = AppSettings.get(INVOICE_ADDRESS,Address.class, new Address());
		addressController.saveToAddress(addr);
		AppSettings.setString(INVOICE_ADDRESS, addr, Address.class);
		AppSettings.setString(KLEINUNTERNEHMER_INFO, kleinunternehmerTextInput.getText());
		AppSettings.setString(INVOICE_ADDITIONAL_TEXT, zusatztextInput.getText());
		AppSettings.setString(VAT_ID, ustIdInput.getText());
		AppSettings.setString(BANK_INFO, new BankInfo(bankNameInput.getText(), ibanInput.getText(), bicInput.getText()), BankInfo.class);
		AppSettings.setBoolean(BANK_INFO_ON_INVOICE, bankOnInvoiceCheckbox.isSelected());
		AppSettings.setBoolean(EMAIL_ON_INVOICE,emailOnInvoiceCheckbox.isSelected());

		AppSettings.setString(AppSettings.APP_DATA_DIRECTORY, appDataDirectoryInput.getText());
		AppSettings.setInt(CURRENT_INVOICE_NUMBER, invoiceNumberInput.getValue()-1);
		
		try {
			AppSettings.setDouble(DEFAULT_TAX, AppUtils.parseDouble(defaultTaxInput.getText(), true));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		initialize();
	}

	public void handleDataExportButton() {
		try {
			AppUtils.exportAppData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleSelectDirectoryButton() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Speicherort für PDF-Rechnungen auswählen");
		File selectedDirectory = dirChooser.showDialog(AppWindow.stage);
		if(selectedDirectory != null)
			appDataDirectoryInput.setText(selectedDirectory.getAbsolutePath());
		
	}
}
