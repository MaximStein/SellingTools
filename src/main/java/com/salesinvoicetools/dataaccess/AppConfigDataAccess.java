package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.AppConfiguration;
import com.salesinvoicetools.models.AppSetting;

public class AppConfigDataAccess extends DataAccessBase {
	
	public static AppConfiguration getAppConfig() {
		AppConfiguration settings = DataAccessBase.getAll(AppConfiguration.class)
				.get(0);		
		return settings;
	}

	public static void setSettingDouble(String key, Double val) {
		setSetting(key, val.toString());
	}

	public static Double getSettingDouble(String key) {
		var s = getSetting(key);

		if(s == null)
			return null;

		return Double.parseDouble(s);
	}

	public static void setSetting(String key, String val) {
		AppSetting setting = (AppSetting) DataAccessBase.find(AppSetting.class, key);
		if(setting == null)
			setting = new AppSetting(key, val);
		setting.value = val;
		DataAccessBase.insertOrUpdate(setting);
	}

	public static String getSetting(String key) {
		AppSetting setting = (AppSetting) DataAccessBase.find(AppSetting.class, key);
		return setting == null ? null : setting.value;
	}
}
