/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2006
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

package net.databinder.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Provides access to application-bound {@link EntityManagerFactory} current
 * {@link EntityManager}. This class will work with a
 * ManagedEntityManagerContext and DataRequestCycle listener when present, but
 * neither is required so long as a "current" {@link EntityManager} is available
 * from the {@link EntityManagerFactory} supplied by the application.
 * @see JPAApplication
 * @author Nathan Hamblen
 * @author fbencosme@kitsd.com
 */
public class Databinder {

  /**
   * Default persistence unit name.
   */
  public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "persistenceUnit";

  /**
   * @return default {@link EntityManagerFactory}, as returned by the
   *         application
   * @throws WicketRuntimeException if {@link EntityManagerFactory} can not be
   *           found
   * @see JPAApplication
   */
  public static EntityManagerFactory getEntityManagerFactory() {
    return getEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT_NAME);
  }

  /**
   * @param key object, or null for the default factory
   * @return {@link EntityManagerFactory}, as returned by the application
   * @throws WicketRuntimeException if {@link EntityManagerFactory} can not be
   *           found
   * @see JPAApplication
   */
  public static EntityManagerFactory getEntityManagerFactory(final String key) {
    final Application app = Application.get();
    if (app instanceof JPAApplication) {
      return ((JPAApplication) app).getEntityManagerFactory(key);
    }
    throw new WicketRuntimeException(
    "Please implement JPAApplication in your Application subclass.");
  }

  public static EntityManagerContext getEntityManagerContext() {
    return getEntityManagerContext(DEFAULT_PERSISTENCE_UNIT_NAME);
  }

  public static EntityManagerContext getEntityManagerContext(final String key) {
    final Application app = Application.get();
    if (app instanceof JPAApplication) {
      return ((JPAApplication) app).getEntityManagerContext(key);
    }
    throw new WicketRuntimeException(
    "Please implement JPAApplication in your Application subclass.");
  }

  /**
   * @return default JPA {@link EntityManager} bound to current thread
   */
  public static EntityManager getEntityManager() {
    dataEntityManagerRequested(DEFAULT_PERSISTENCE_UNIT_NAME);
    return getEntityManagerContext().currentEntityManager();
  }

  /**
   * @param persistenceUnit or null for the default factory
   * @return {@link EntityManager} bound to current thread
   */
  public static EntityManager getEntityManager(final String persistenceUnit) {
    dataEntityManagerRequested(persistenceUnit);
    return getEntityManagerFactory(persistenceUnit).createEntityManager();
  }

  /**
   * @return true if a EntityManager is bound for the default factory
   */
  public static boolean hasBoundEntityManager() {
    return hasEntityManagerBound(DEFAULT_PERSISTENCE_UNIT_NAME);
  }

  /**
   * @param persistenceUnit or null for the default factory
   * @return true if a {@link EntityManager} is bound for the keyed factory
   */
  public static boolean hasEntityManagerBound(final String persistenceUnit) {
    return getEntityManagerContext(persistenceUnit).hasBind();
  }

  /**
   * Notifies current request cycle that a data {@link EntityManager} was
   * requested, if a entity manager was not already bound for this thread and
   * the request cycle is an DataRequestCycle.
   * @param persistenceUnitName or null for the default factory
   * @see JPARequestCycle
   */
  private static void dataEntityManagerRequested(final String persistenceUnitName) {
    if (!hasEntityManagerBound(persistenceUnitName)) {
      // if entity manager is unavailable, it could be a late-loaded
      // conversational
      // cycle
      final RequestCycle cycle = RequestCycle.get();
      if (cycle instanceof JPARequestCycle) {
        ((JPARequestCycle) cycle)
        .dataEntityManagerRequested(persistenceUnitName);
      }
    }
  }

  /**
   * Wraps EntityManagerUnit callback in a temporary thread-bound
   * {@link EntityManager} from the default factory if necessary. This is to be
   * used outside of a regular a {@link EntityManager}-handling request cycle,
   * such as during application init or an external Web service request. The
   * temporary EntityManager and transaction, if created, are closed after the
   * callback returns and uncommited transactions are rolled back. Be careful of
   * returning detached JPA objects that may not be fully loaded with data;
   * consider using projections / scalar queries instead.<b>Note</b> This method
   * uses a ManagedEntityManagerContext. With JTA or other forms of current
   * {@link EntityManager} lookup a wrapping {@link EntityManager} will not be
   * detected and a new one will always be created.
   * @param unit work to be performed in thread-bound EntityManager
   * @see EntityManagerUnit
   */
  public static Object ensureEntityManager(final EntityManagerUnit unit) {
    return ensureEntityManager(unit, null);
  }

  /**
   * Wraps EntityManagerUnit callback in a temporary thread-bound JPA
   * {@link EntityManager} from the keyed factory if necessary. This is to be
   * used outside of a regular a {@link EntityManager}-handling request cycle,
   * such as during application init or an external Web service request. The
   * temporary EntityManager and transaction, if created, are closed after the
   * callback returns and uncommited transactions are rolled back. Be careful of
   * returning detached JPA 0 * objects that may not be fully loaded with data;
   * consider using projections / scalar queries instead. <b>Note</b> This
   * method uses a ManagedEntityManagerContext. With JTA or other forms of
   * current {@link EntityManager} lookup a wrapping {@link EntityManager} will
   * not be detected and a new one will always be created.
   * @param unit work to be performed in thread-bound {@link EntityManager}
   * @param key or null for the default factory
   * @see EntityManagerUnit
   */
  public static Object ensureEntityManager(final EntityManagerUnit unit,
      final String key) {
    dataEntityManagerRequested(key);
    final EntityManagerContext emc = getEntityManagerContext(key);
    if (emc.hasBind()) {
      return unit.run(getEntityManager(key));
    }

    final EntityManager em = getEntityManagerFactory(key).createEntityManager();
    try {
      em.getTransaction().begin();
      emc.bind(em);
      return unit.run(em);
    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
        emc.unbind();
      }
    }
  }
}
