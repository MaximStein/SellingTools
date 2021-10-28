package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.AppConfiguration;

public class AppConfigDataAccess extends DataAccessBase {
	
	public static AppConfiguration getAppConfig() {
		AppConfiguration settings = DataAccessBase.getAll(AppConfiguration.class)
				.get(0);		
		return settings;
	}	
}
