package com.salesinvoicetools.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.NoResultException;
import javax.persistence.criteria.*;

import com.google.api.client.util.Strings;

import com.salesinvoicetools.dataaccess.models.ShopOrdersFilterModel;
import com.salesinvoicetools.models.DataSource;
import com.salesinvoicetools.models.OrderInvoice;
import com.salesinvoicetools.models.ShopOrder;
import com.salesinvoicetools.utils.Pagination;
import javafx.scene.control.TableColumn.SortType;
import com.salesinvoicetools.shopapis.ShopApiBase.*;

public class OrderDataAccess extends DataAccessBase {

	public static long GetOrderCount() {
		var q = em.createQuery("SELECT COUNT(o) FROM ShopOrder o");

		return (long) q.getSingleResult();
	}

	public static void insertOrUpdateInvoiceData(ShopOrder order, long invoiceNumber) {
		
		if (order.getInvoice() == null) {
			order.setInvoice(new OrderInvoice());
		}
		
		order.getInvoice().setInvoiceNumber(invoiceNumber);
		order.getInvoice().setInvoiceTime(Timestamp.from(Instant.now()));
		DataAccessBase.insertOrUpdate(order);
	}

	public static Long getInvoiceNumber(ShopOrder o) {
		return o.getInvoice() == null ? null : o.getInvoice().getInvoiceNumber();
	}

	public static void insertOrder(ShopOrder o) {
		em.getTransaction().begin();
		em.persist(o);
		em.getTransaction().commit();
	}

	public static ShopOrder getByOrderNumber(String orderNumber, Marketplace p) {
		var orders = DataAccessBase.getWhere(ShopOrder.class, "orderNumber", orderNumber);
		orders = orders.stream()
				.filter(o -> (p == null && (o.getDataSource() == null || o.getDataSource().token == null))
						|| (o.getDataSource() != null && o.getDataSource().token.owner.platform == p))
				.collect(Collectors.toList());
		return orders.size() == 0 ? null : orders.get(0);
	}

	public static List<ShopOrder> getAllOrders(Pagination p) {
		var q = em.createQuery("SELECT o FROM ShopOrder o", ShopOrder.class);
		q.setFirstResult(p.getOffset());
		q.setMaxResults(p.pageSize);
		return q.getResultList();
	}

	private static List<Predicate> getPredicates(ShopOrdersFilterModel filter, Root<ShopOrder> root) {
		List<Predicate> filterPredicates = new ArrayList<>();
		var builder = em.getCriteriaBuilder();
		Join<OrderInvoice, ShopOrder> invoice = root.join("invoice", JoinType.LEFT);

		if (!Strings.isNullOrEmpty(filter.searchText)) {
			var t = "%" + filter.searchText + "%";
			filterPredicates.add(builder.or(builder.like(root.get("orderNumber"), t),
					builder.and(builder.isNotNull(invoice), builder.like(invoice.get("invoiceNumber"), t)),
					builder.like(root.get("buyer").get("userName"), t),
					builder.like(root.get("buyerCheckoutMessage"), t)));
		}

		if (filter.onlyWithoutInvoice) {
			filterPredicates.add(builder.isNull(invoice));
		}

		if (filter.orderStartDate != null) {
			filterPredicates.add(builder.greaterThan(root.<Date>get("orderTime"),
					Timestamp.valueOf(filter.orderStartDate.atStartOfDay())));
		}

		if (filter.filterByToken != null && filter.filterByToken.size() > 0) {
			Join<DataSource, ShopOrder> dataUpdate = root.join("dataSource", JoinType.LEFT);
			var tokens = filter.filterByToken;
			var tokenFilterPredicates = new ArrayList<Predicate>();

			tokens.stream().forEach(t -> {
				if(t.token != null) {
					tokenFilterPredicates.add(builder.equal(dataUpdate.get("token"), t.token));
				} else {
					tokenFilterPredicates.add(builder.isNull(root.get("dataSource")));
				}
			});

			filterPredicates.add(builder.or(tokenFilterPredicates.toArray(new Predicate[0])));
		}

		return filterPredicates;
	}

	public static long count(ShopOrdersFilterModel filter) {

		var qb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = qb.createQuery(Long.class);
		Root<ShopOrder> root = cq.from(ShopOrder.class);
		cq.select(qb.count(root));
		cq.where(getPredicates(filter, root).toArray(Predicate[]::new));
		return em.createQuery(cq).getSingleResult();
	}

	public static List<ShopOrder> getPage(ShopOrdersFilterModel filter) {

		CriteriaQuery<ShopOrder> query = getAllQuery(ShopOrder.class);
		Root<ShopOrder> root = query.from(ShopOrder.class);
		query.select(root);
		var builder = em.getCriteriaBuilder();

		var predicates = getPredicates(filter, root);

		query.where(builder.and(predicates.toArray(Predicate[]::new)));

		if (Strings.isNullOrEmpty(filter.sortField)) {
			query.orderBy(builder.desc(root.get("orderNumber")));
		} else {
			Path<Object> path = root.get(filter.sortField);
			query.orderBy(filter.sortType == SortType.ASCENDING ? builder.asc(path) : builder.desc(path));
		}

		return em.createQuery(query).setFirstResult(filter.page * filter.entriesPerPage)
				.setMaxResults(filter.entriesPerPage).getResultList();
	}
}
