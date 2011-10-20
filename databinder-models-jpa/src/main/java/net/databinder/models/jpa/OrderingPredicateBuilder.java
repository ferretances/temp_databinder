package net.databinder.models.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.Predicate;

import net.databinder.util.CriteriaDefinition;

/**
 * Builds predicate objects with or without order. Only one of the build methods
 * should be called in building a predicate object.
 */
public interface OrderingPredicateBuilder<T> extends Serializable {

  public OrderingPredicateBuilder<T> setCriteriaDefinition(
      CriteriaDefinition<T> criteriaDefinition);

  public CriteriaDefinition<T> getCriteriaDefinition();

  /** Build the criteria without setting an order */
  public void buildUnordered(List<Predicate> predicates);

  /** Build the (entire) criteria, including an order */
  public void buildOrdered(List<Predicate> predicates);
}
