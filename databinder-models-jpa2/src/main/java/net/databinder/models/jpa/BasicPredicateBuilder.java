package net.databinder.models.jpa;

import java.util.List;

import javax.persistence.criteria.Predicate;

import net.databinder.util.CriteriaDefinition;

/**
 * DefaultPredicateBuilder is responsible of
 * @author fbencosme@kitsd.com
 * @param <T>
 */
public abstract class BasicPredicateBuilder<T> implements PredicateBuilder<T> {

  private static final long serialVersionUID = 1L;

  private CriteriaDefinition<T> criteriaDefinition;

  public BasicPredicateBuilder(final Class<T> entityClass) {
    criteriaDefinition = new CriteriaDefinition<T>(entityClass);
  }

  public BasicPredicateBuilder(final CriteriaDefinition<T> criteriaDefinition) {
    this.criteriaDefinition = criteriaDefinition;
  }

  public PredicateBuilder<T> setCriteriaDefinition(
      final CriteriaDefinition<T> criteriaDefinition) {
    this.criteriaDefinition = criteriaDefinition;
    return this;
  }

  public CriteriaDefinition<T> getCriteriaDefinition() {
    return criteriaDefinition;
  }

  public void build(final List<Predicate> predicates) {
  }

}
