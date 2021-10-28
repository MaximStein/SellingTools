package com.salesinvoicetools.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.salesinvoicetools.models.Address;

public class AddressController {

	@FXML 
	TextField nameInput;
	
	@FXML 
	TextField addressAdditionInput;
	
	@FXML 
	TextField streetInput;
	
	@FXML 
	TextField zipInput;
	
	@FXML 
	TextField countryInput;
	
	@FXML 
	TextField cityInput;
	
	
	/**
	 * inserts the address data into the appropriate input fields
	 * @param a
	 */
	public void setAddress(Address a) {
		nameInput.setText(a.getName());
		addressAdditionInput.setText(a.getAdditionalInfo());
		streetInput.setText(a.getStreet());
		zipInput.setText(a.getPostalCode());
		cityInput.setText(a.getCity());
		countryInput.setText(a.getCountry());
		
	}

	/**
	 * disables all inputs
	 * @param b
	 */
	public void setDisabled(boolean b) {
		nameInput.setDisable(b);
		addressAdditionInput.setDisable(b);
		streetInput.setDisable(b);
		zipInput.setDisable(b);
		countryInput.setDisable(b);
		cityInput.setDisable(b);
	}

	/**
	 * saves the form data to the address object
	 * @param a 
	 */
	public void saveToAddress(Address a) {
		a.setName(nameInput.getText());
		a.setCity(cityInput.getText());
		a.setAdditionalInfo(addressAdditionInput.getText());
		a.setPostalCode(zipInput.getText());
		a.setCountry(countryInput.getText());
		a.setStreet(streetInput.getText());
	}
	
}

