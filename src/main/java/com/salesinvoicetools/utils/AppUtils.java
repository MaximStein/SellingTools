package com.salesinvoicetools.utils;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.salesinvoicetools.dataaccess.AppConfigDataAccess;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.models.ShopOrder;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.util.converter.NumberStringConverter;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class AppUtils {
	
	public static NumberStringConverter numberStringConverter = new NumberStringConverter(Locale.GERMAN);

	private static ShopOrder find(long id ) {
		return (ShopOrder) DataAccessBase.find(ShopOrder.class, id);
	}
	
	
	/**
	 * generates invoices for the provided order-IDs in the saved output directory, shows a confirmation dialog
	 * @param selectedShopOrderIds
	 */
	public static void generateOrderInvoices(Long[] selectedShopOrderIds) {
		
		if(selectedShopOrderIds.length > 1) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setContentText(
					"PDFs f�r " + selectedShopOrderIds.length + " Bestellungen in das Verzeichnis "
					+ AppConfigDataAccess.getAppConfig().getInvoiceDirectory()+" erzeugen?");
			ButtonType okButton = new ButtonType("Ok", ButtonData.OK_DONE);
			ButtonType cancelButton = new ButtonType("Abbrechen", ButtonData.CANCEL_CLOSE);
			alert.getButtonTypes().setAll(okButton, cancelButton);
			
			alert.showAndWait().ifPresent(type -> {
				if (type == okButton) {
					Arrays.stream(selectedShopOrderIds).forEach(id -> AppUtils.generateOrderInvoice(id));
				}
			});			
		} 
		else {
			Arrays.stream(selectedShopOrderIds).forEach(id -> AppUtils.generateOrderInvoice(id));
		}		
	}
	
	/**
	 * generates an invoice inside the user-defined output directory, showing an error alert if none is defined
	 * @param orderId
	 * @return the invoice number
	 */
	public static Long generateOrderInvoice(long orderId) {
		
		var config = AppConfigDataAccess.getAppConfig();
		if(Strings.isNullOrEmpty(config.getInvoiceDirectory())) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setContentText(
					"Es wurde noch kein Zielverzeichnis f�r PDF-Dateien in den Einstellungen festgelegt");
			alert.show();	
		}
		
		ShopOrder order = find(orderId);
		if(order == null)
			return null;

		OrderDataAccess.getInvoiceNumber(order);
		
		long invoiceNumber;
		
		if(order.getInvoice() == null) {
			invoiceNumber = config.getIncrementedInvoiceNumber();
		}
		else {
			invoiceNumber = order.getInvoice().getInvoiceNumber();
		}		
		
		
		if (!PDFUtils.createInvoiceFile(order, config, invoiceNumber)) {			
			if(order.getInvoice() == null)
				config.setCurrentInvoiceNumber(config.getCurrentInvoiceNumber()-1);
			
			var message = "Die Datei konnte nicht erstellt werden."
					+ "\r\nBitte pr�fen Sie die Schreibberechtigungen f�r das Zielverzeichnis:\r\n"
					+ config.getInvoiceDirectory();
			new Alert(AlertType.ERROR, message).show();
			return null;
		} 
		else {
			OrderDataAccess.insertOrUpdateInvoiceData(order, invoiceNumber);
			DataAccessBase.insertOrUpdate(config);
			return invoiceNumber;
		}		
	}

	/**
	 * 
	 * @param urlStr the URL-string
	 * @param name the parameter name
	 * @return the value of the URL-parameter specified, null if not present
	 * @throws MalformedURLException
	 */
	public static String getParameterVal(String urlStr, String name) throws MalformedURLException {
		
		var url = new URL(urlStr);
		var parts = url.getQuery().split("&");						
		for(var part : parts) {
			var keyVal = part.split("=");
			
			if(keyVal[0].equals(name))
				return keyVal[1];
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param cents
	 * @return the currency-formatted amount, e.g. 12,49 �
	 */
	public static String formatCurrencyAmount(long cents) {
		return String.format("%.2f", 1d*cents / 100)+" �";
	}
	
	/**
	 * 
	 * @param ts
	 * @return the full date and time value with hours and seconds
	 */
	public static String formatDateTime(Timestamp ts) {
		return ts == null ? "" : new SimpleDateFormat("dd.MM.yyyy hh:mm").format(ts);
	}
	
	/**
	 * 
	 * @param ts
	 * @return the date without time portion
	 */
	public static String formatDate(Timestamp ts) {
		return ts == null ? "" : new SimpleDateFormat("dd.MM.yyyy").format(ts);
	}
	
	/**
	 * 
	 * @param d
	 * @return the double value in German number format
	 */
	public static String formatDouble(Double d) {
		return numberStringConverter.toString(d);
	}
	
	/**
	 * 
	 * @param str the value in German number format
	 * @param emptyIsZero return 0 if str is empty
	 * @return double val for the provided str
	 * @throws ParseException
	 */
	public static double parseDouble(String str, boolean emptyIsZero) throws ParseException {
		
		if(emptyIsZero && str.trim().length() == 0)
			return 0d;
		
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		Number number = format.parse(str);
		
		return number.doubleValue();		
	}

	public static String getFilePart(String path) {
		var pattern = Pattern.quote(System.getProperty("file.separator"));
		var parts = path.split(pattern);
		return parts[parts.length-1];
	}
	
	
}
