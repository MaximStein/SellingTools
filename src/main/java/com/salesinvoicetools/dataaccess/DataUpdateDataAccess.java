package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OAuth2Token;
import com.salesinvoicetools.models.Product;
import com.salesinvoicetools.models.ShopOrder;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;

public class DataUpdateDataAccess extends DataAccessBase {
	
	public static void removeUnusedUpdates(OAuth2Token source) {
		var builder = em.getCriteriaBuilder();
		CriteriaQuery<DataSource> query = builder.createQuery(DataSource.class);
		Root<DataSource> root = query.from(DataSource.class);
		query.select(root);		
		query.where(builder.equal(root.get("token"), source));
		query.orderBy(builder.desc(root.<Timestamp>get("time")));
		
		var updates = em.createQuery(query).getResultList();				
		updates.stream().forEach(u -> 
		{
			if(DataAccessBase.countWhere(ShopOrder.class, Collections.singletonMap("dataSource", u)) == 0)
				DataAccessBase.delete(u);
		});					
	}
	
	public static Collection<DataSource> getByProductNumber(String productNumber) {

		var updates = new HashSet<DataSource>();
		var products = DataAccessBase.getWhere(Product.class, "productNumber", productNumber);

		products.stream().forEach(p -> {
			p.getLineItems().stream().forEach(l -> {
				updates.add(l.getOwner().getDataSource());
			});
		});
		var list = new ArrayList<DataSource>(updates);
		list.sort((a, b) -> b.getTime().compareTo(a.getTime()) );
		
		return list;
	}
	
	public static List<DataSource> getUpdatesForToken(OAuth2Token t) {
		var builder = em.getCriteriaBuilder();
		CriteriaQuery<DataSource> query = builder.createQuery(DataSource.class);
		Root<DataSource> root = query.from(DataSource.class);
		query.select(root);		
		query.where(builder.equal(root.get("token"), t));
		query.orderBy(builder.desc(root.<Timestamp>get("time")));		
		return em.createQuery(query).getResultList();
	}
	
}
