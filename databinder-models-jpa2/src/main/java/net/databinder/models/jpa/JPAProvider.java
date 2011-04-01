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

import net.databinder.jpa.Databinder;
import net.databinder.models.PropertyDataProvider;
import net.databinder.util.CriteriaDefinition;

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
  private OrderingPredicateBuilder<T> orderingPredicateBuilder;
  private QueryBuilder queryBuilder;
  private QueryBuilder countQueryBuilder;

  private String factoryKey;
  private CriteriaDefinition<T> criteriaDefinition;

  /**
   * Provides all entities of the given class.
   */
  public JPAProvider(final Class<T> objectClass) {
    this.entityClass = objectClass;
    criteriaDefinition =
      new CriteriaDefinition<T>(objectClass, Databinder.getEntityManager());
  }

  /** Provides entities of the given class meeting the supplied criteria. */
  public JPAProvider(final Class<T> objectClass,
      final PredicateBuildAndSort<T> predicateOrderingBuilder,
      final String orderProperty) {
    this(objectClass, new OrderingPredicateBuilder<T>() {

      private static final long serialVersionUID = 1L;
      private CriteriaDefinition<T> criteriaDefinition;

      @Override
      public void buildUnordered(final List<Predicate> predicates) {
        predicateOrderingBuilder.buildUnordered(predicates);
      }

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        predicateOrderingBuilder.buildOrdered(criteria);
      }

      @Override
      public OrderingPredicateBuilder<T> setCriteriaDefinition(
          final CriteriaDefinition<T> criteriaDefinition) {
        this.criteriaDefinition = criteriaDefinition;
        return this;
      }

      @Override
      public CriteriaDefinition<T> getCriteriaDefinition() {
        return criteriaDefinition;
      }
    });
  }

  /**
   * Provides all entities of the given class using a distinct criteria builder
   * for the order query.
   * @param entityClass
   * @param predicateBuilder base criteria builder
   * @param predicateOrderingBuilder add ordering information ONLY, base
   *          criteria will be called first
   */
  public JPAProvider(final Class<T> entityClass,
      final PredicateBuilder<T> predicateBuilder,
      final PredicateBuilder<T> predicateOrderingBuilder,
      final String orderProperty) {
    this(entityClass);
    criteriaDefinition =
      new CriteriaDefinition<T>(entityClass, Databinder.getEntityManager());
    this.orderingPredicateBuilder = new OrderingPredicateBuilder<T>() {
      private static final long serialVersionUID = 1L;

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        criteriaDefinition.getCriteriaQuery().orderBy(
            criteriaDefinition.getCriteriaBuilder().asc(
                criteriaDefinition.getRoot().get(orderProperty)));
      }

      @Override
      public void buildUnordered(final List<Predicate> criteria) {
        criteriaDefinition.getCriteriaQuery().orderBy(
            criteriaDefinition.getCriteriaBuilder().asc(
                criteriaDefinition.getRoot().get(orderProperty)));
      }

      @Override
      public OrderingPredicateBuilder<T> setCriteriaDefinition(
          final CriteriaDefinition<T> criteriaDefinition) {
        return this;
      }

      @Override
      public CriteriaDefinition<T> getCriteriaDefinition() {
        return criteriaDefinition;
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
      final OrderingPredicateBuilder<T> criteriaBuider) {
    this(objectClass);
    this.orderingPredicateBuilder = criteriaBuider;
  }

  /** Provides entities of the given class meeting the supplied criteria. */
  public JPAProvider(final Class<T> objectClass,
      final net.databinder.models.jpa.PredicateBuilder<T> criteriaBuilder) {
    this(objectClass, new OrderingPredicateBuilder<T>() {
      private static final long serialVersionUID = 1L;
      private CriteriaDefinition<T> criteriaDefinition;

      @Override
      public void buildUnordered(final List<Predicate> criteria) {
        criteriaBuilder.build(criteria);
      }

      @Override
      public void buildOrdered(final List<Predicate> criteria) {
        criteriaBuilder.build(criteria);
      }

      @Override
      public OrderingPredicateBuilder<T> setCriteriaDefinition(
          final CriteriaDefinition<T> criteriaDefinition) {
        this.criteriaDefinition = criteriaDefinition;
        return this;
      }

      @Override
      public CriteriaDefinition<T> getCriteriaDefinition() {
        return criteriaDefinition;
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
    final CriteriaDefinition<T> cq = getCriteriaDefinition();
    if (queryBuilder != null) {
      cq.select();
      final Query q = queryBuilder.build(cq.getEntityManager());
      q.setFirstResult(first);
      q.setMaxResults(count);
      return q.getResultList().iterator();
    }

    final List<Predicate> predicates = new ArrayList<Predicate>();
    if (orderingPredicateBuilder != null) {
      orderingPredicateBuilder.buildOrdered(predicates);
    }
    if (queryBuilder != null) {
      queryBuilder.build(cq.getEntityManager());
    }
    cq.select();
    cq.perform();

    final TypedQuery<T> query = cq.getTypeQuery();
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

    if (orderingPredicateBuilder != null) {
      orderingPredicateBuilder.buildUnordered(predicates);
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

  public CriteriaDefinition<T> getCriteriaDefinition() {
    return criteriaDefinition;
  }
}
