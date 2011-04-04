package net.databinder.jpa;

/**
 * Request cycle that should be notified on the first use of a data EntityManager.
 */
public interface JPARequestCycle {

  public void dataEntityManagerRequested(final String persistenceUnitName);
}
