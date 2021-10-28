package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;

public class ApiAccessDataAccess extends DataAccessBase {
	
	public static void deleteToken(OAuth2Token entry) {
		em.getTransaction().begin();
		
		DataAccessBase.getAll(DataSource.class).forEach(da -> {
			if(da.getToken() == entry)
				em.remove(da);
			});
	
		entry.getOwner().getTokens().remove(entry);
		
		em.getTransaction().commit();		
	}

	public static void addToken(ApiAccess parent, OAuth2Token entry) {
		em.getTransaction().begin();
		if(!parent.getTokens().contains(entry))
			parent.getTokens().add(entry);		
		em.getTransaction().commit();
	}

	public static void removeToken(ApiAccess parent, OAuth2Token token) {
		em.getTransaction().begin();
		parent.getTokens().remove(token);
		em.getTransaction().commit();
	}
}
