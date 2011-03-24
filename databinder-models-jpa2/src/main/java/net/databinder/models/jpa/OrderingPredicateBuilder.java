package net.databinder.models.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.Predicate;

/**
 * Builds criteria objects with or without order. Only one of the build methods
 * should be called in building a criteria object.
 */
public interface OrderingPredicateBuilder extends Serializable {
  /** Build the criteria without setting an order */
  public void buildUnordered(List<Predicate>  criteria);
  /** Build the (entire) criteria, including an order */
  public void buildOrdered(List<Predicate> criteria);
}
