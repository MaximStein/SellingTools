package com.salesinvoicetools.dataaccess;

import com.google.api.client.util.Strings;
import com.google.gson.Gson;
import com.salesinvoicetools.models.AppConfiguration;
import com.salesinvoicetools.models.AppSetting;

public abstract class AppSettings extends DataAccessBase {

	public static final String CURRENT_INVOICE_NUMBER = "business.currentInvoiceNumber";
	public static final String INVOICE_OUTPUT_DIRECTORY = "business.invoiceOutputDirectory";
	public static final String INVOICE_ADDRESS = "business.invoiceAddress";
	public static final String KLEINUNTERNEHMER_INFO = "business.kleinunternehmerInfo";
	public static final String BANK_INFO_ON_INVOICE = "business.bankInfoOnInvoice";
	public static final String BANK_INFO = "business.bankInfo";
	public static final String VAT_ID = "business.vatId";
	public static final String CONTACT_INFO = "business.contactInfo";
	public static final String INVOICE_ADDITIONAL_TEXT = "business.invoiceAdditionalText";
	public static final String DEFAULT_TAX = "business.defaultTax";
	public static final String EMAIL_ON_INVOICE = "business.emailOnInvoice";
	public static final String TOKEN_COLOR_ = "shopApi.tokenColor";
    public static final String APP_DATA_DIRECTORY = "system.appDataDirectory";


    public static <T> T get(String key, Class<T> type, T defaultValue) {
		return getString(key) == null ? defaultValue : get(key,type);
	}

	public static <T> T get(String key, Class<T> type) {
		String content = AppSettings.getString(key);
		if(content == null || Strings.isNullOrEmpty(content))
			return null;

		Gson gson = new Gson();
		return gson.fromJson(AppSettings.getString(key), type);
	}

	public static AppConfiguration getAppConfig() {
		AppConfiguration settings = DataAccessBase.getAll(AppConfiguration.class)
				.get(0);		
		return settings;
	}

	public static void setDouble(String key, Double val) {
		setString(key, val.toString());
	}

	public static void setBoolean(String key, Boolean val) {
		setString(key, val.toString());
	}

	public static void setInt(String key, Integer val) {
		setString(key, val.toString());
	}

	public static Boolean getBoolean(String key, Boolean defaultValue) {
		return getBoolean(key) == null ? defaultValue : getBoolean(key);
	}

	public static Boolean getBoolean(String key) {
		return getString(key) == null ? null : Boolean.valueOf(getString(key)) || getString(key).equals("1");
	}

	public static Double getDouble(String key) {
		var s = getString(key);

		if(s == null)
			return null;

		return Double.parseDouble(s);
	}

	public static Integer getInt(String key, Integer defaultValue) {
		return getString(key) == null ? defaultValue : getInt(key);
	}

	public static Integer getInt(String key) {
		var s = getString(key);

		if(s == null)
			return null;

		return Integer.parseInt(s);
	}

	public static void setString(String key, Object val, Class c) {
		Gson gson = new Gson();
		AppSettings.setString(key, gson.toJson(val,c));
	}
	public static void setString(String key, String val) {
		AppSetting setting = (AppSetting) DataAccessBase.find(AppSetting.class, key);
		if(setting == null)
			setting = new AppSetting(key, val);
		setting.value = val;
		DataAccessBase.insertOrUpdate(setting);
	}

	public static String getString(String key, String defaultValue) {
		return getString(key) == null ? defaultValue : getString(key);
	}

	public static String getString(String key) {
		AppSetting setting = (AppSetting) DataAccessBase.find(AppSetting.class, key);
		return setting == null ? null : setting.value;
	}
}
