package net.databinder.jpa;

import javax.persistence.EntityManagerFactory;

/**
 * Databinder application interface. DataStaticService expects the current
 * Wicket application to conform to this interface and supply a entity manager
 * factory as needed.
 * @see Databinder
 * @author Fausto Bencosme
 */
public interface JPAApplication {

  /**
   * Supply the entity manager factory for the given persistenceUnitName.
   * Applications needing only one entity manager factory may return it without
   * inspecting the key parameter.
   * @param key or null for the default factory
   * @return configured {@link EntityManagerFactory}
   */
  EntityManagerFactory getEntityManagerFactory(final String key);

  /**
   * Supply the entity manager context for the given persistenceUnitName.
   * Applications needing only one entity manager context may return it without
   * inspecting the key parameter.
   * @param key or null for the default factory
   * @return the appropriate {@link EntityManagerContext}
   */
  EntityManagerContext getEntityManagerContext(final String key);

}