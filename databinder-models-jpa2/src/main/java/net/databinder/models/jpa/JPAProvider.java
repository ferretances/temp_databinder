/*
 * Databinder: a simple bridge from Wicket to Hibernate Copyright (C) 2006
 * Nathan Hamblen nathan@technically.us This library is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version. This library is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.databinder.models.jpa;

import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import net.databinder.jpa.Databinder;
import net.databinder.models.PropertyDataProvider;

import org.apache.wicket.model.IModel;

/**
 * Provides query results to DataView and related components. Like the Hibernate
 * model classes, the results of this provider can be altered by query binders
 * and criteria builders. By default this provider wraps items in a compound
 * property model in addition to a Hibernate model. This is convenient for
 * mapping DataView subcomponents as bean properties (as with PropertyListView).
 * However, <b>DataTable will not work with a compound property model.</b> Call
 * setWrapWithPropertyModel(false) when using with DataTable, DataGridView, or
 * any other time you do not want a compound property model.
 * @author Nathan Hamblen
 */
public class JPAProvider<T> extends PropertyDataProvider<T> {

  private static final long serialVersionUID = 1L;

  private Class<T> objectClass;
  private OrderingCriteriaBuilder criteriaBuilder;
  private QueryBuilder queryBuilder, countQueryBuilder;

  private String factoryKey;

  /**
   * Provides all entities of the given class.
   */
  public JPAProvider(final Class<T> objectClass) {
    this.objectClass = objectClass;
  }

  /**
   * Provides all entities of the given class using a distinct criteria builder
   * for the order query.
   * @param entityClass
   * @param criteriaBuilder base criteria builder
   * @param criteriaOrderer add ordering information ONLY, base criteria will be
   *          called first
   */
  public JPAProvider(final Class<T> entityClass,
      final CriteriaBuilder criteriaBuilder,
      final CriteriaBuilder criteriaOrderer) {
    this(entityClass);
    this.criteriaBuilder = new OrderingCriteriaBuilder() {
      /** */
      private static final long serialVersionUID = 1L;

      @Override
      public void buildOrdered(
          final javax.persistence.criteria.CriteriaBuilder cb) {
        final CriteriaQuery<T> criteria = cb.createQuery(entityClass);
        final Root<T> e = criteria.from(entityClass);
        criteria.select(e);
        criteria.orderBy(cb.asc(e.get("id")));
      }

      @Override
      public void buildUnordered(
          final javax.persistence.criteria.CriteriaBuilder cb) {
        final CriteriaQuery<T> cq = cb.createQuery(entityClass);
        final Root<T> e = cq.from(entityClass);
        cq.select(e);
        cq.orderBy(cb.desc(e.get("id")));
      }
    };
  }

  /**
   * Provides all entities of the given class.
   * @param objectClass
   * @param criteriaBuider builds different criteria objects for iterator() and
   *          size()
   */
  public JPAProvider(final Class<T> objectClass,
      final OrderingCriteriaBuilder criteriaBuider) {
    this(objectClass);
    this.criteriaBuilder = criteriaBuider;
  }

  /** Provides entities of the given class meeting the supplied criteria. */
  public JPAProvider(final Class<T> objectClass,
      final net.databinder.models.jpa.CriteriaBuilder criteriaBuilder) {
    this(objectClass, new OrderingCriteriaBuilder() {
      /** */
      private static final long serialVersionUID = 1L;

      @Override
      public void buildOrdered(
          final javax.persistence.criteria.CriteriaBuilder criteria) {
        criteriaBuilder.build(criteria);
      }

      @Override
      public void buildUnordered(
          final javax.persistence.criteria.CriteriaBuilder criteria) {
        criteriaBuilder.build(criteria);
      }
    });
  }

  /**
   * Provides entities matching the given query. The count query is derived by
   * prefixing "select count(*)" to the given query; this will fail if the
   * supplied query has a select clause.
   */
  public JPAProvider(final String query) {
    this(query, makeCount(query));
  }

  /**
   * Provides entities matching the given queries.
   */
  public JPAProvider(final String query, final String countQuery) {
    this(new QueryBinderBuilder(query), new QueryBinderBuilder(countQuery));
  }

  /**
   * Provides entities matching the given query with bound parameters. The count
   * query is derived by prefixing "select count(*)" to the given query; this
   * will fail if the supplied query has a select clause.
   * @deprecated because the derived count query is often non-standard, even if
   *             it works. Use the longer constructor.
   */
  @Deprecated
  public JPAProvider(final String query, final QueryBinder queryBinder) {
    this(query, queryBinder, makeCount(query), queryBinder);
  }

  /**
   * Provides entities matching the given queries with bound parameters.
   * @param query query to return entities
   * @param queryBinder binder for the standard query
   * @param countQuery query to return count of entities
   * @param countQueryBinder binder for the count query (may be same as
   *          queryBinder)
   */
  public JPAProvider(final String query, final QueryBinder queryBinder,
      final String countQuery, final QueryBinder countQueryBinder) {
    this(new QueryBinderBuilder(query, queryBinder), new QueryBinderBuilder(
        countQuery, countQueryBinder));
  }

  public JPAProvider(final QueryBuilder queryBuilder,
      final QueryBuilder countQueryBuilder) {
    this.queryBuilder = queryBuilder;
    this.countQueryBuilder = countQueryBuilder;
  }

  /**
   * @deprecated
   * @return query with select count(*) prepended
   */
  @Deprecated
  static protected String makeCount(final String query) {
    return "select count(*) " + query;
  }

  /** @return session factory key, or null for the default factory */
  public String getFactoryKey() {
    return factoryKey;
  }

  /**
   * Set a factory key other than the default (null).
   * @param key session factory key
   * @return this, for chaining
   */
  public JPAProvider<T> setFactoryKey(final String key) {
    this.factoryKey = key;
    return this;
  }

  /**
   * It should not normally be necessary to override (or call) this default
   * implementation.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<T> iterator(final int first, final int count) {
    final EntityManager em = Databinder.getEntityManager(factoryKey);

    if (queryBuilder != null) {
      final Query q = queryBuilder.build(em);
      q.setFirstResult(first);
      q.setMaxResults(count);
      return q.getResultList().iterator();
    }

    if (criteriaBuilder != null) {
      criteriaBuilder.buildOrdered(em.getCriteriaBuilder());
    }

    final Query q = queryBuilder.build(em);
    q.setFirstResult(first);
    q.setMaxResults(count);
    return q.getResultList().iterator();
  }

  /**
   * Only override this method if a single count query or criteria projection is
   * not possible.
   */
  @Override
  public int size() {
    final EntityManager em = Databinder.getEntityManager(getFactoryKey());
    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    cq.select(cb.count(cq.from(objectClass)));

    if (countQueryBuilder != null) {
      final Query q = countQueryBuilder.build(em);
      final Object obj = q.getSingleResult();
      return ((Number) obj).intValue();
    }

    if (criteriaBuilder != null) {
      criteriaBuilder.buildUnordered(em.getCriteriaBuilder());
    }
    final Query q = countQueryBuilder.build(em);
    final Object obj = q.getSingleResult();
    return ((Number) obj).intValue();
  }

  @Override
  protected IModel<T> dataModel(final T object) {
    return new JPAObjectModel<T>(object);
  }

  /** does nothing */
  @Override
  public void detach() {
  }
}
