/*
 * Copyright 2011 Kindleit Technologies. All rights reserved. This file, all
 * proprietary knowledge and algorithms it details are the sole property of
 * Kindleit Technologies unless otherwise specified. The software this file
 * belong with is the confidential and proprietary information of Kindleit
 * Technologies. ("Confidential Information"). You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Kindleit.
 */
package net.databinder.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import net.databinder.jpa.Databinder;

/**
 * CriteriaDefinition is responsible of building a jpa2 basic configuration as:
 * <ul>
 *  <li> Root
 *  <li> CriteriaQuery
 *  <li> CriteriaBuilder
 * 
 * @author fbencosme@kitsd.com
 * @param <T> entity.
 */
public class CriteriaDefinition<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Root<T> root;

  private final CriteriaQuery<Object> criteriaQuery;

  private final CriteriaBuilder criteriaBuilder;

  private final Class<T> entityClass;

  private List<Predicate> predicates = new ArrayList<Predicate>();

  public CriteriaDefinition(final Class<T> entityClass) {
    this.entityClass = entityClass;
    criteriaBuilder = getEntityManager().getCriteriaBuilder();
    criteriaQuery = criteriaBuilder.createQuery();
    root = criteriaQuery.from(entityClass);
  }

  public Root<T> getRoot() {
    return root;
  }

  public Class<T> getEntityClass() {
    return entityClass;
  }

  public CriteriaBuilder getCriteriaBuilder() {
    return criteriaBuilder;
  }

  public CriteriaQuery<Object> getCriteriaQuery() {
    return criteriaQuery;
  }

  public EntityManager getEntityManager() {
    return Databinder.getEntityManager();
  }

  public CriteriaDefinition<T> setPredicates(final List<Predicate> predicates) {
    this.predicates = predicates;
    return this;
  }

  public List<Predicate> getPredicates() {
    return predicates;
  }

  public void addPredicate(final Predicate predicate) {
    predicates.add(predicate);
  }

  public void removePredicate(final Predicate predicate) {
    predicates.remove(predicate);
  }

  public void addAllPredicates(final List<Predicate> predicates) {
    this.predicates.addAll(predicates);
  }

  public void cleanLikePredicates() {
    predicates.clear();
  }

  public void perform() {
    criteriaQuery.where(criteriaBuilder.and(predicates
        .toArray(new Predicate[0])));
  }

  public CriteriaQuery<Object> selectAll() {
    criteriaQuery.select(root).distinct(true);
    return criteriaQuery;
  }

  @SuppressWarnings("unchecked")
  public TypedQuery<T> getTypeQuery () {
    return (TypedQuery<T>) getEntityManager().createQuery(criteriaQuery);
  }


}
