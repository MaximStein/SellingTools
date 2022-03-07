package com.salesinvoicetools.utils;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.DateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesinvoicetools.controllers.AppController;
import com.salesinvoicetools.dataaccess.AppSettings;
import static com.salesinvoicetools.dataaccess.AppSettings.*;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.dataaccess.OrderDataAccess;
import com.salesinvoicetools.models.*;
import com.salesinvoicetools.shopapis.ShopApiBase;
import com.salesinvoicetools.viewmodels.AppDataExport;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.Notifications;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
					"PDFs für " + selectedShopOrderIds.length + " Bestellungen in Verzeichnis "
					+ PDFUtils.getInvoiceDirectory()+" speichern?");
			ButtonType okButton = new ButtonType("Ok", ButtonData.OK_DONE);
			ButtonType cancelButton = new ButtonType("Abbrechen", ButtonData.CANCEL_CLOSE);
			alert.getButtonTypes().setAll(okButton, cancelButton);

			alert.showAndWait().ifPresent(type -> {
				if (type == okButton) {
					var map = new HashMap<Long, ShopOrder>();
					Arrays.stream(selectedShopOrderIds).forEach(id -> {
						var order = (ShopOrder)DataAccessBase.find(ShopOrder.class, id);
						var invoiceNumber = AppUtils.getOrGenerateInvoiceNumber(order);
						map.put(invoiceNumber,order);
						OrderDataAccess.insertOrUpdateInvoiceData(order, invoiceNumber);
					});
					//Arrays.stream(selectedShopOrderIds).forEach(id -> AppUtils.generateOrderInvoice(id));
					if(PDFUtils.createInvoicesFile(map)) {
						AppUtils.showNotificationInfo("fertig", "Rechnungen wurden erstellt" );
					}
					else {
						AppUtils.showNotificationInfo("fertig", "Rechnungen konnten nicht erstellt werden");
					}
				}
			});			
		} 
		else {
			Arrays.stream(selectedShopOrderIds).forEach(id -> AppUtils.generateOrderInvoice(id));
		}		
	}


	public static long getOrGenerateInvoiceNumber(ShopOrder order) {
		Integer invoiceNumber = AppSettings.getInt(CURRENT_INVOICE_NUMBER,0);

		if(order.getInvoice() == null) {
			invoiceNumber++;
			AppSettings.setInt(CURRENT_INVOICE_NUMBER, invoiceNumber);
		}
		else {
			invoiceNumber = Math.toIntExact(order.getInvoice().getInvoiceNumber());
		}

		return invoiceNumber;
	}

	public static Long parseCurrency(String str) throws ParseException {
		var newStr = str.contains("€") ? str : str+"€";
		var val = parseDouble(newStr.split("€",1)[0], true);
		return (long)(val * 100l);
	}

	public static boolean importAppData(File file) throws IOException {
		Gson gson = new Gson();
		var appData = gson.fromJson(Files.readString(file.toPath()), AppDataExport.class);

		appData.businessData.forEach((k, v) -> {
			if(v == null || v.equals("null"))
				return;
			AppSettings.setString(k,v);
		});

		appData.apiAccessData.forEach(apiAccess -> {
			ApiAccess api = (ApiAccess) DataAccessBase.find(ApiAccess.class, apiAccess.platform);

			if(api == null)
			{
				AppSettings.insertOrUpdate(apiAccess);
				return;
			}

			api.clientId = apiAccess.clientId;
			api.clientSecret = apiAccess.clientSecret;
			api.callbackUrl = apiAccess.callbackUrl;
			api.customData = apiAccess.customData;

			ApiAccess finalApi = api;
			apiAccess.tokens.forEach(t -> {
					var existingToken = finalApi.tokens.stream()
							.filter(t1 -> t1.name.equals(t.name))
							.findFirst().get();

					if(existingToken == null){
						finalApi.tokens.add(t);
						AppSettings.insertOrUpdate(finalApi);
						return;
					}
					existingToken.accessToken = t.accessToken;
					existingToken.refreshToken = t.refreshToken;
					existingToken.acessTokenExpirationTime = t.acessTokenExpirationTime;
					existingToken.refreshTokenExpirationTime = t.refreshTokenExpirationTime;
			});
		});

		return true;
	}

	public static void exportAppData() throws IOException {
		ArrayList<String> keys = new ArrayList<>();

		var businessDataKeys = new String[] {
				INVOICE_ADDRESS, CURRENT_INVOICE_NUMBER, BANK_INFO, BANK_INFO_ON_INVOICE, KLEINUNTERNEHMER_INFO,
				VAT_ID, DEFAULT_TAX, EMAIL_ON_INVOICE, INVOICE_ADDITIONAL_TEXT, CONTACT_INFO
		};

		var businessData = new HashMap<String, String>();

		Arrays.stream(businessDataKeys).forEach(k -> {
			businessData.put(k, AppSettings.getString(k));
		});

		var exportDataModel = new AppDataExport();
		exportDataModel.businessData = businessData;
		exportDataModel.apiAccessData = DataAccessBase.getAll(ApiAccess.class);

		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String json = gson.toJson(exportDataModel);

		var targetDir = AppSettings.getString(APP_DATA_DIRECTORY, ".");
		var fileName = (Timestamp.from(Instant.now())+"")
				.replace(":", "")
				.replace(" ","")
				.replace(".","")+".json";

		File file = new File(targetDir+File.separator+fileName);
		AppUtils.log("schreibe Export nach "+file.getCanonicalPath());
		FileUtils.writeStringToFile(file, json, Charset.defaultCharset());
	}

	/**
	 * generates an invoice inside the user-defined output directory, showing an error alert if none is defined
	 * @param orderId
	 * @return the invoice number
	 */
	public static Long generateOrderInvoice(long orderId) {
		

		/*if(Strings.isNullOrEmpty(AppSettings.getString(APP_DATA_DIRECTORY))) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setContentText(
					"Es wurde noch kein Zielverzeichnis für PDF-Dateien in den Einstellungen festgelegt");
			alert.show();	
		} */
		
		ShopOrder order = find(orderId);
		if(order == null)
			return null;

		//OrderDataAccess.getInvoiceNumber(order);
		var invoiceNumber = AppUtils.getOrGenerateInvoiceNumber(order);

		if (!PDFUtils.createInvoiceFile(order, invoiceNumber)) {
			if(order.getInvoice() == null)
				AppSettings.setInt(AppSettings.CURRENT_INVOICE_NUMBER, (int) (invoiceNumber-1));
			
			var message = "Die Datei konnte nicht erstellt werden."
					+ "\r\nBitte prüfen Sie die Schreibberechtigungen für das Zielverzeichnis:\r\n"
					+ PDFUtils.getInvoiceDirectory();
			new Alert(AlertType.ERROR, message).show();
			return null;
		} 
		else {
			OrderDataAccess.insertOrUpdateInvoiceData(order, invoiceNumber);

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
		if(url.getQuery() == null)
			return null;
		var parts = url.getQuery().split("(//?|&)");
		for(var part : parts) {
			var keyVal = part.split("=");
			
			if(keyVal[0].equals(name))
				return keyVal[1];
		}
		
		return null;
	}

	public static String formatCurrencyAmount(Long cents, String currencySymbol) {
		if(cents == null )
			return null;

		return String.format("%.2f", 1d*cents / 100)+" "+currencySymbol;
	}
	/**
	 * 
	 * @param cents
	 * @return the currency-formatted amount, e.g. 12,49 €
	 */
	public static String formatCurrencyAmount(Long cents) {
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


	/**
	 *
	 * @param i
	 * @return the full date and time value with hours and seconds
	 */
	public static String formatTimeOfDay(Instant i) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss")
				.withZone(ZoneId.systemDefault());


		return i == null ? "" : dtf.format(i);
	}

	public static String shortenString(String str, int leftLenght, int rightLength) {
		var newString = "";

		if(str.length() <= leftLenght+rightLength)
			return str;

		rightLength = Math.min(str.length() - leftLenght, rightLength);

		newString += str.substring(0, leftLenght)+".."+str.substring(str.length()-rightLength);

		return newString;
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
	public static Double parseDouble(String str, boolean emptyIsZero) throws ParseException {
		
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

	public static String getFileFormat(String path) {
		if(!path.contains("."))
			return null;
		return path.substring(path.lastIndexOf(".")+1);
	}

	public static String getFilePart(String path) {
		var pattern = Pattern.quote(System.getProperty("file.separator"));
		var parts = path.split(pattern);
		return parts[parts.length-1];
	}



	public static void showNotificationError(String title, String content) {

	}

	public static void showNotificationInfo(String title, String content) {
		AppUtils.showNotification(title,content, NotificationType.INFO );
	}

	public enum NotificationType { INFO, ERROR,CONFIRM, WARNING}
	public static void showNotification(String title, String content, NotificationType t) {
			var notification = Notifications.create()
					.title(title)
					.text(content)
					.position(Pos.BOTTOM_CENTER)
					.hideAfter(Duration.seconds(7));
			switch(t) {
				case ERROR:
					notification.showError();
				break;
				case INFO:
					notification.showInformation();
					break;
				case CONFIRM:
					notification.showConfirm();
					break;
				case WARNING:
					notification.showWarning();
					break;
			}
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


	public static void persistChanges(IndexedCheckModel val, String key) {

		var current = AppSettings.getString(key);

		val.clearChecks();
		if(current != null) {
			 Arrays.stream(current.split(","))
					.forEach(str -> {
						try {
							val.check(Integer.parseInt(str));
						}
						catch(NumberFormatException e) {}

					});
		}

		val.getCheckedIndices().addListener(new ListChangeListener<Integer>() {
			@Override
			public void onChanged(Change<? extends Integer> change) {
				var vals = val.getCheckedIndices().stream().map(i -> ""+i).collect(Collectors.joining(","));
				AppSettings.setString(key, (String) vals);
			}
		});
	}

	public static void persistChangesInt(IntegerProperty val, String key, Integer defaultValue) {
		val.addListener(((observableValue, o, n) -> {
			AppSettings.setInt(key, n.intValue());
		}));
		var currentValue = AppSettings.getDouble(key);
		val.setValue(currentValue == null ? defaultValue : currentValue);
	}

	public static void persistChangesDouble(DoubleProperty val, String key, Double defaultValue) {
		val.addListener(((observableValue, o, n) -> {
			AppSettings.setDouble(key, n.doubleValue());
		}));
		var currentValue = AppSettings.getDouble(key);
		val.setValue(currentValue == null ? defaultValue : currentValue);
	}

	public static void log(String message) {
		log(message,Color.LIGHT_GRAY);
	}

	public static void log(String message, Color color) {

		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		var className = stackTraceElements[2].getClassName();

		try {
			var c =  Class.forName(className);

			if(AppController.instance != null)
				AppController.instance.log(String.format("#%08X", color.getRGB()),className,message);

		} catch ( ClassNotFoundException e) {
			if(AppController.instance != null)
				AppController.instance.log(className,message);
		}

		System.out.println("["+className+"]: "+message);
		System.out.print(ConsoleColors.RESET);

	}
	public static void log(String msg, int indents, Color color) {
		log(Strings.repeat("	",indents)+msg, color);
	}

	public static void log(String msg, int indents) {
		log(msg, indents, Color.lightGray);
	}

	public static String toRGBCode( javafx.scene.paint.Color color )
	{
		return String.format( "#%02X%02X%02X",
				(int)( color.getRed() * 255 ),
				(int)( color.getGreen() * 255 ),
				(int)( color.getBlue() * 255 ) );
	}

	public static void persistChangesInt(ObjectProperty<Integer> val, String key,Integer defaultValue) {
		val.addListener(((observableValue, o, n) -> {
			AppSettings.setString(key, n.toString());
		}));

		var currentValue = AppSettings.getInt(key);
		val.setValue(currentValue == null ? defaultValue : currentValue);
	}


	public static WritableImage convertWritableImage(BufferedImage bf){
		WritableImage wr = null;
		if (bf != null) {
			wr = new WritableImage(bf.getWidth(), bf.getHeight());
			PixelWriter pw = wr.getPixelWriter();
			for (int x = 0; x < bf.getWidth(); x++) {
				for (int y = 0; y < bf.getHeight(); y++) {
					pw.setArgb(x, y, bf.getRGB(x, y));
				}
			}
		}
		return wr;
	}

	public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = resizedImage.createGraphics();
		graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		graphics2D.dispose();
		return resizedImage;
	}

	public static void deleteImageFromCache(String imageName, ImageSize imgSize) throws IOException {
		var file = getCacheImageLocation(imageName, imgSize);
		new File(file).delete();
	}

	public static String getCacheImageLocation(String file, ImageSize size) throws IOException {
		var appDir = AppSettings.getString(APP_DATA_DIRECTORY, new File(".").getCanonicalPath())+"/";
		var relativeFileName = file.startsWith(appDir) ? file.substring(appDir.length()) : file;
		var cacheDir = new File(appDir+"/cache/"+size);
		var convertedFileName = getCacheConvertedFileName(relativeFileName);

		return cacheDir +File.separator+convertedFileName;
	}

	public static String getCacheConvertedFileName(String originalFile) {
		return originalFile
				.replace(File.separator, "--")
				.replace(":","-")
				.replace("/", "_");
	}


	public static BufferedImage readImageFile(String fileName) throws IOException {
		var f = new File(fileName);
		if(f.exists())
			return ImageIO.read(f);
		return null;
	}

	public enum ImageSize { SM, ORIGINAL }
	public static BufferedImage getImageFromCache(String fileName, ImageSize size) throws IOException {
		return readImageFile(getCacheImageLocation(fileName, size));
	}

	public static void cacheImage(String fileName, ImageSize size) throws IOException {

		var cacheDir =  getCacheImageLocation(fileName, size);
		BufferedImage bufferedImage = null;

		if(!new File(cacheDir).exists()){
			Files.createDirectories(Path.of(cacheDir.substring(0,cacheDir.lastIndexOf(File.separator))));

			if(fileName.startsWith("http://") || fileName.startsWith("https://")) {
				bufferedImage =  ImageIO.read(new URL(fileName));
			}
			else if(Files.exists(Path.of(fileName))) {
				bufferedImage = ImageIO.read(new File(fileName));
			}
			else {
				return;
			}

			ImageIO.write(AppUtils.resizeImage(bufferedImage,60,60),
					AppUtils.getFileFormat(fileName),
					new File(cacheDir));
		}
	}

	public static String mapToJson(Map<String,String> map) {
		var ref = new Object() {
			String str = "{";
		};

		map.forEach((o, o2) -> {
			ref.str += "\""+o+"\":\""+o2+"\",";
		});

		if(ref.str.endsWith(","))
			ref.str = ref.str.substring(0,ref.str.length()-1);

		return ref.str+"}";

	}

	public static Map<String,String> jsonToMap(String str) {
		Gson gson = new Gson();
		Map map = gson.fromJson(str, Map.class);
		return map;
	}
}