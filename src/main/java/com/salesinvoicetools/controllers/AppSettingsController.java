package com.salesinvoicetools.controllers;


import com.salesinvoicetools.AppWindow;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.models.AppConfiguration;
import com.salesinvoicetools.utils.AppUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.text.ParseException;

public class AppSettingsController {

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
	TextField invoiceDirectoryInput;

	@FXML 
	Spinner<Integer> invoiceNumberInput;
	
	private AppConfiguration settings;

	public void initialize() {

		defaultTaxInput.setTextFormatter(new TextFormatter<>(AppUtils.numberStringConverter));

		settings = (AppConfiguration) DataAccessBase.getAll(AppConfiguration.class).get(0);

		addressController.setAddress(settings.getAddress());
		kleinunternehmerTextInput.setText(settings.getKleinunternehmerInfoText());
		zusatztextInput.setText(settings.getAdditionalText());
		ustIdInput.setText(settings.getVatNumber());
		bankNameInput.setText(settings.getBankInfo().getBankName());
		ibanInput.setText(settings.getBankInfo().getIban());
		bicInput.setText(settings.getBankInfo().getBic());
		bankOnInvoiceCheckbox.setSelected(settings.isBankInfoOnInvoice());
		emailOnInvoiceCheckbox.setSelected(settings.isEmailOnInvoice());
		invoiceDirectoryInput.setText(settings.getInvoiceDirectory());
		defaultTaxInput.setText(AppUtils.numberStringConverter.toString(settings.getDefaultTax()));
		
		invoiceNumberInput.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, settings.getCurrentInvoiceNumber()+1));		

	}

	public void handleSaveButton() {
		addressController.saveToAddress(settings.getAddress());
		settings.setKleinunternehmerInfoText(kleinunternehmerTextInput.getText());
		settings.setAdditionalText(zusatztextInput.getText());
		settings.setVatNumber(ustIdInput.getText());
		settings.getBankInfo().setBankName(bankNameInput.getText());
		settings.getBankInfo().setIban(ibanInput.getText());
		settings.getBankInfo().setBic(bicInput.getText());
		settings.setBankInfoOnInvoice(bankOnInvoiceCheckbox.isSelected());
		settings.setEmailOnInvoice(emailOnInvoiceCheckbox.isSelected());
		settings.setInvoiceDirectory(invoiceDirectoryInput.getText());
		settings.setCurrentInvoiceNumber(invoiceNumberInput.getValue()-1);
		
		try {
			settings.setDefaultTax(AppUtils.parseDouble(defaultTaxInput.getText(), true));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DataAccessBase.insertOrUpdate(settings);
		initialize();
	}
	
	public void handleSelectDirectoryButton() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Speicherort f�r PDF-Rechnungen ausw�hlen");		
		File selectedDirectory = dirChooser.showDialog(AppWindow.stage);
		if(selectedDirectory != null)
			invoiceDirectoryInput.setText(selectedDirectory.getAbsolutePath());
		
	}
}
