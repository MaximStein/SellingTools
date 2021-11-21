package com.salesinvoicetools.dataaccess;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DataAccessBase {

	protected static EntityManager em;

	static {
		setEntityManager("h2-localdb");
	}

	public static void setEntityManager(String persistenceUnitName) {
		var emf = Persistence.createEntityManagerFactory(persistenceUnitName);
		em = emf.createEntityManager();
	}

	protected static <T> CriteriaQuery<T> getAllQuery(Class<T> c) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(c);

		query.from(c);

		return query;
	}
	
	public static void flush() {
		em.flush();
	}

	private static <T> Predicate[] getPredicates(Map<String, Object> where, Root<T> root) {
		List<Predicate> preds = new ArrayList<>();

		var builder = em.getCriteriaBuilder();

		where.forEach((k, v) -> {
			preds.add(builder.equal(root.get(k), v));
		});

		return preds.toArray(Predicate[]::new);
	}

	public static long countWhere(Class<?> c, Map<String, Object> where) {
		var qb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = qb.createQuery(Long.class);
		Root<?> root = cq.from(c);
		cq.where(qb.and(DataAccessBase.getPredicates(where, root)));
		cq.select(qb.count(root));
		return em.createQuery(cq).getSingleResult();
	}

	public static <T> T getOneWhere(Class<T> c, String field, Object value) {
		var entries = getWhere(c, field, value);
		if (entries.size() == 0)
			return null;
		return entries.get(0);
	}



	public static <T> List<T> getWhereAnd(Class<T> c, Map<String, Object> where) {
		return getWhere(c,where,false);
	}

	public static <T> List<T> getWhereOr(Class<T> c, Map<String, Object> where) {
		return getWhere(c,where,true);
	}

	private static <T> List<T> getWhere(Class<T> c, Map<String, Object> where, boolean or) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(c);
		var root = query.from(c);

		if (where.size() == 1)
			query.where(getPredicates(where, root));
		else
			query.where(or ? builder.or(getPredicates(where, root)) : builder.and(getPredicates(where, root)));

		return em.createQuery(query).getResultList();
	}



	public static <T> List<T> getWhere(Class<T> c, String field, Object value) {
		return getWhere(c, Collections.singletonMap(field, value), false);
	}

	public static <T> List<T> getPage(Class<T> c, int pageIndex, int pageSize) {
		CriteriaQuery<T> query = getAllQuery(c);

		return em.createQuery(query).setFirstResult(pageIndex * pageSize).setMaxResults(pageSize).getResultList();
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getAll(Class<T> c) {
		var entries = em.createQuery(DataAccessBase.<T>getAllQuery(c)).getResultList();
		return entries;
	}

	public static long count(Class<?> c) {
		em.getTransaction().begin();
		var result = (long) em.createQuery("SELECT COUNT(o) FROM " + c.getName() + " o").getSingleResult();
		em.getTransaction().commit();
		return result;
	}

	public static boolean exists(Class<?> c, Object entityPK) {
		return em.find(c, entityPK) != null;
	}

	public static void delete(Object entry) {
		em.getTransaction().begin();
		em.remove(entry);
		em.getTransaction().commit();

	}

	public static Object insertOrUpdate(Object entry) {
		em.getTransaction().begin();
		if (em.contains(entry))
			em.merge(entry);
		else
			em.persist(entry);
		em.getTransaction().commit();
		return entry;

	}

	public static Object find(Class<?> c, Object pk) {
		Object entry = em.find(c, pk);
		return entry;
	}

}
