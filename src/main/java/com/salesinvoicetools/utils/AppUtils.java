package com.salesinvoicetools.utils;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.salesinvoicetools.controllers.AppController;
import com.salesinvoicetools.dataaccess.AppConfigDataAccess;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.models.Address;
import com.salesinvoicetools.models.ShopOrder;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.Notifications;
import com.salesinvoicetools.shopapis.ShopApiBase.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
		var parts = url.getQuery().split("(//?|&)");
		for(var part : parts) {
			var keyVal = part.split("=");
			
			if(keyVal[0].equals(name))
				return keyVal[1];
		}
		
		return null;
	}

	public static String formatCurrencyAmount(long cents, String currencySymbol) {
		return String.format("%.2f", 1d*cents / 100)+" "+currencySymbol;
	}
	/**
	 * 
	 * @param cents
	 * @return the currency-formatted amount, e.g. 12,49 �
	 */
	public static String formatCurrencyAmount(long cents) {
		return formatCurrencyAmount(cents, "EUR");
	}
	
	/**
	 * 
	 * @param ts
	 * @return the full date and time value with hours and seconds
	 */
	public static String formatDateTime(Timestamp ts) {
		return ts == null ? "" : new SimpleDateFormat("dd.MM.yyyy hh:mm").format(ts);
	}

	public static String formatDateTime(Instant i) {
		return i == null ? "" : new SimpleDateFormat("dd.MM.yyyy hh:mm").format(Timestamp.from(i));
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

	public static Calendar intantToCalendar(Instant i) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
		Calendar cal = GregorianCalendar.from(zdt);
		return cal;
	}

	public static String toHexString(byte[] hash)
	{
		BigInteger number = new BigInteger(1, hash);
		StringBuilder hexString = new StringBuilder(number.toString(16));
		while (hexString.length() < 32)
			hexString.insert(0, '0');
		return hexString.toString();
	}


	public static String encodeBase64(String str) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(str.getBytes(StandardCharsets.UTF_8));
	}

	public static String getFilePart(String path) {
		var pattern = Pattern.quote(System.getProperty("file.separator"));
		var parts = path.split(pattern);
		return parts[parts.length-1];
	}
	
	public static void showNotification(String title, String content) {
		Notifications.create()
				.title(title)
				.text(content)
				.hideAfter(Duration.seconds(7))
				.show();
	}


	public static Address.Country parseCountry(String str) {

		var strLower = str.toLowerCase();

		switch(strLower) {
			case "germany":
			case "deutschland":
			case "ger":
				return Address.Country.DE;
			case "united kingdom":
			case "vereinigtes königreich":
			case "england":
				return Address.Country.UK;
			case "niederlande":
			case "netherlands":
				return Address.Country.NL;
			case "france":
			case "frankreich":
				return Address.Country.FR;
			case "usa":
			case "amerika":
			case "united states of america":
			case "united states":
			case "america":
				return Address.Country.USA;

		}

		try {
			return Address.Country.valueOf(str);
		}
		catch(IllegalArgumentException e) {
			return Address.Country.OTHER;
		}
	}

	public static void persistChanges(IndexedCheckModel<Marketplace> val, String key) {

		var current = AppConfigDataAccess.getSetting(key);

		val.clearChecks();
		if(current != null) {
			 Arrays.stream(current.split(","))
					.forEach(str -> val.check(Integer.parseInt(str)));
		}

		val.getCheckedIndices().addListener(new ListChangeListener<Integer>() {
			@Override
			public void onChanged(Change<? extends Integer> change) {
				var vals = val.getCheckedIndices().stream().map(i -> ""+i).collect(Collectors.joining(","));
				AppConfigDataAccess.setSetting(key, vals);
			}
		});
	}

	public static void persistChangesDouble(DoubleProperty val, String key, Double defaultValue) {
		val.addListener(((observableValue, o, n) -> {
			AppConfigDataAccess.setSettingDouble(key, n.doubleValue());
		}));
		var currentValue = AppConfigDataAccess.getSettingDouble(key);
		val.setValue(currentValue == null ? defaultValue : currentValue);
	}

	public static void log(String message) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		var className = stackTraceElements[2].getClassName();

		try {
			var c =  Class.forName(className);
			var f = c.getField("LOG_COLOR");
			var clr = (String)f.get(null);
			System.out.print(clr);

			AppController.instance.log(clr,className,message);

		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
			AppController.instance.log(className,message);
			//System.err.println("couldnt apply color to class "+className);
		}

		System.out.println("["+className+"]: "+message);
		System.out.print(ConsoleColors.RESET);

	}

	public static void log(String msg, int indents) {
		log(Strings.repeat("	",indents)+msg);
	}
}
