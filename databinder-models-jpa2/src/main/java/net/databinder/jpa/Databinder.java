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

package net.databinder.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;

/**
 * Provides access to application-bound Hibernate session factories and current
 * sessions. This class will work with a ManagedEntityManagerContext and
 * DataRequestCycle listener when present, but neither is required so long as a
 * "current" session is available from the session factory supplied by the
 * application.
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
   * @return default session factory, as returned by the application
   * @throws WicketRuntimeException if session factory can not be found
   * @see JPAApplication
   */
  public static EntityManagerFactory getEntityManagerFactory() {
    return getEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT_NAME);
  }

  /**
   * @param key object, or null for the default factory
   * @return entity manager factory, as returned by the application
   * @throws WicketRuntimeException if session factory can not be found
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

  /**
   * @return default Hibernate session bound to current thread
   */
  public static EntityManager getEntityManager() {
    final Application app = Application.get();
    if (app instanceof JPAApplication) {
      return ((JPAApplication) app)
      .getEntityManager(DEFAULT_PERSISTENCE_UNIT_NAME);
    }
    throw new WicketRuntimeException(
    "Please implement JPAApplication in your Application subclass.");
  }

  /**
   * @param persistenceUnit or null for the default factory
   * @return {@link EntityManager} bound to current thread
   */
  public static EntityManager getEntityManager(final String persistenceUnit) {
    dataSessionRequested(persistenceUnit);
    return getEntityManagerFactory(persistenceUnit).createEntityManager();
  }

  /**
   * @return true if a session is bound for the default factory
   */
  public static boolean hasBoundEntityManager() {
    return hasEntityManagerBound(DEFAULT_PERSISTENCE_UNIT_NAME);
  }

  /**
   * @param persistenceUnit or null for the default factory
   * @return true if a session is bound for the keyed factory
   */
  public static boolean hasEntityManagerBound(final String persistenceUnit) {
    return ManagedEntityManagerContext
    .hasBind(getEntityManagerFactory(persistenceUnit));
  }

  /**
   * Notifies current request cycle that a data session was requested, if a
   * entity manager was not already bound for this thread and the request cycle
   * is an DataRequestCycle.
   * @param persistenceUnitName or null for the default factory
   * @see JPARequestCycle
   */
  private static void dataSessionRequested(final String persistenceUnitName) {
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
   * Wraps SessionUnit callback in a temporary thread-bound Hibernate session
   * from the default factory if necessary. This is to be used outside of a
   * regular a session-handling request cycle, such as during application init
   * or an external Web service request. The temporary session and transaction,
   * if created, are closed after the callback returns and uncommited
   * transactions are rolled back. Be careful of returning detached Hibernate
   * objects that may not be fully loaded with data; consider using projections
   * / scalar queries instead.<b>Note</b> This method uses a
   * ManagedSessionContext. With JTA or other forms of current session lookup a
   * wrapping session will not be detected and a new one will always be created.
   * @param unit work to be performed in thread-bound session
   * @see EntityManagerUnit
   */
  public static Object ensureSession(final EntityManagerUnit unit) {
    return ensureSession(unit, null);
  }

  /**
   * Wraps SessionUnit callback in a temporary thread-bound Hibernate session
   * from the keyed factory if necessary. This is to be used outside of a
   * regular a session-handling request cycle, such as during application init
   * or an external Web service request. The temporary session and transaction,
   * if created, are closed after the callback returns and uncommited
   * transactions are rolled back. Be careful of returning detached Hibernate
   * objects that may not be fully loaded with data; consider using projections
   * / scalar queries instead. <b>Note</b> This method uses a
   * ManagedSessionContext. With JTA or other forms of current session lookup a
   * wrapping session will not be detected and a new one will always be created.
   * @param unit work to be performed in thread-bound session
   * @param key or null for the default factory
   * @see EntityManagerUnit
   */
  public static Object ensureSession(final EntityManagerUnit unit,
      final String key) {
    dataSessionRequested(key);
    final EntityManagerFactory emf = getEntityManagerFactory(key);
    if (ManagedEntityManagerContext.hasBind(emf)) {
      return unit.run(getEntityManager(key));
    }
    final EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      ManagedEntityManagerContext.bind(em);
      return unit.run(em);
    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
        ManagedEntityManagerContext.unbind(emf);
      }
    }
  }
}