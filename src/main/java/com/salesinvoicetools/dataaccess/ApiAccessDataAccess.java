package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.ApiAccess;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;

public class ApiAccessDataAccess extends DataAccessBase {
	
	public static void deleteToken(OAuth2Token entry) {
		em.getTransaction().begin();
		
		DataAccessBase.getAll(DataSource.class).forEach(da -> {
			if(da.getToken() == entry)
				System.out.println("Removing DataSource "+da.id);
				em.remove(da);
			});

		if(entry.getOwner().tokens.remove(entry)) {
			System.out.println("removed token "+entry.id+" from ApiAccess "+entry.owner.platform);
		}
		else {
			System.out.println("token "+entry.id+" not found in ApiAccess "+entry.owner.platform);
		}

		System.out.println("Removing token "+entry.id+" from DB");
		em.remove(entry);
		em.getTransaction().commit();

	}

	public static void addToken(ApiAccess parent, OAuth2Token entry) {
		em.getTransaction().begin();
		if(!parent.tokens.contains(entry))
			parent.tokens.add(entry);
		em.getTransaction().commit();
	}

	public static void removeToken(ApiAccess parent, OAuth2Token token) {
		em.getTransaction().begin();
		parent.tokens.remove(token);
		em.getTransaction().commit();
	}
}
