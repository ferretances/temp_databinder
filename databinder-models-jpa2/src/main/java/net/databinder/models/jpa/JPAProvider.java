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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
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

  /** */
  private static final long serialVersionUID = 1L;
  private Class<T> entityClass;
  private OrderingPredicateBuilder criteriaBuilder;
  private QueryBuilder queryBuilder, countQueryBuilder;

  private String factoryKey;

  final CriteriaBuilder cb;
  final CriteriaQuery<T> cq;
  final Root<T> root;

  /**
   * Provides all entities of the given class.
   */
  public JPAProvider(final Class<T> objectClass) {
    this.entityClass = objectClass;
    final EntityManager em = Databinder.getEntityManager();
    cb = em.getCriteriaBuilder();
    cq = cb.createQuery(entityClass);
    root = cq.from(entityClass);
  }

  /** Provides entities of the given class meeting the supplied criteria. */
  public JPAProvider(final Class<T> objectClass,
      final PredicateBuildAndSort<T> criteriaBuilder, final String orderProperty) {
    this(objectClass, new OrderingPredicateBuilder() {

      private static final long serialVersionUID = 1L;

      @Override
      public void buildUnordered(final List<Predicate> criteria) {
        criteriaBuilder.buildUnordered(criteria);
      }

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        criteriaBuilder.buildOrdered(criteria);
      }
    });
  }

  /**
   * Provides all entities of the given class using a distinct criteria builder
   * for the order query.
   * @param entityClass
   * @param predicateBuilder base criteria builder
   * @param predicateOrderingBuilder add ordering information ONLY, base criteria
   *          will be called first
   */
  public JPAProvider(final Class<T> entityClass,
      final PredicateBuilder<T> predicateBuilder,
      final PredicateBuilder<T> predicateOrderingBuilder,
      final String orderProperty) {
    this(entityClass);
    cq.select(root);

    this.criteriaBuilder = new OrderingPredicateBuilder() {
      private static final long serialVersionUID = 1L;

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        cq.orderBy(cb.asc(root.get(orderProperty)));
      }

      @Override
      public void buildUnordered(final List<Predicate> criteria) {
        cq.orderBy(cb.asc(root.get(orderProperty)));
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
      final OrderingPredicateBuilder criteriaBuider) {
    this(objectClass);
    this.criteriaBuilder = criteriaBuider;
  }

  /** Provides entities of the given class meeting the supplied criteria. */
  public JPAProvider(final Class<T> objectClass,
      final net.databinder.models.jpa.PredicateBuilder<T> criteriaBuilder) {
    this(objectClass, new OrderingPredicateBuilder() {
      private static final long serialVersionUID = 1L;

      @Override
      public void buildUnordered(final List<Predicate> criteria) {
        criteriaBuilder.build(criteria);
      }

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        criteriaBuilder.build(criteria);
      }
    });
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
    final EntityManager em = Databinder.getEntityManager();
    cq.select(root);
    if (queryBuilder != null) {
      final Query q = queryBuilder.build(em);
      q.setFirstResult(first);
      q.setMaxResults(count);
      return q.getResultList().iterator();
    }

    final List<Predicate> predicates = new ArrayList<Predicate>();
    if (criteriaBuilder != null) {
      criteriaBuilder.buildOrdered(predicates);
    }
    if (queryBuilder != null) {
      queryBuilder.build(em);
    }
    cq.select(root);
    cq.where(cb.and(predicates.toArray(new Predicate[0])));

    final TypedQuery<T> query = em.createQuery(cq);
    query.setFirstResult(first);
    query.setMaxResults(count);
    return query.getResultList().iterator();
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

    final List<Predicate> predicates = new ArrayList<Predicate>();
    cq.select(cb.count(cq.from(entityClass)));
    if (countQueryBuilder != null) {
      final Query q = countQueryBuilder.build(em);
      final Object obj = q.getSingleResult();
      return ((Number) obj).intValue();
    }

    if (criteriaBuilder != null) {
      criteriaBuilder.buildUnordered(predicates);
    }
    final TypedQuery<Long> query = em.createQuery(cq);
    return query.getSingleResult().intValue();
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
