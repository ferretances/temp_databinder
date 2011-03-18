package net.databinder.models.jpa;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;

/**
 * Builds criteria objects with or without order. Only one of the build methods
 * should be called in building a criteria object.
 */
public interface OrderingCriteriaBuilder extends Serializable {
  /** Build the criteria without setting an order */
  public void buildUnordered(CriteriaBuilder criteria);
  /** Build the (entire) criteria, including an order */
  public void buildOrdered(CriteriaBuilder criteria);
}
