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

/*
 * Note: this class contains code adapted from wicket-contrib-database.
 */

package net.databinder.jpa;

import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import net.databinder.CookieRequestCycle;

import org.apache.wicket.Page;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Opens JPA em and transactions as required and closes them at a
 * request's end. Uncomitted transactions are rolled back. Uses keyed JPA
 * session factories from Databinder service.
 * </p>
 * @see Databinder
 * @author Nathan Hamblen
 */
public class DataRequestCycle extends CookieRequestCycle implements
JPARequestCycle {

  /** Keys for session factories that have been opened for this request */
  protected HashSet<String> keys = new HashSet<String>();

  private static final Logger log = LoggerFactory
  .getLogger(DataRequestCycle.class);

  public DataRequestCycle(final WebApplication application,
      final WebRequest request, final Response response) {
    super(application, request, response);
  }

  /** Roll back active transactions and close session. */
  protected void closeEntityManager(final String key) {
    final EntityManager em = Databinder.getEntityManager(key);

    if (em.isOpen()) {
      try {
        if (em.getTransaction().isActive()) {
          log.debug("Rolling back uncomitted transaction.");
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
      }
    }
  }

  /**
   * Called by DataStaticService when a em is needed and does not already
   * exist. Opens a new thread-bound JPA em.
   */
  public void dataEntityManagerRequested(final String key) {
    openEntityManager(key);
  }

  /**
   * Open a session and begin a transaction for the keyed session factory.
   * @param key object, or null for the default factory
   * @return newly opened session
   */
  protected EntityManager openEntityManager(final String key) {
    final EntityManager em = Databinder.getEntityManagerFactory(key).createEntityManager();
    em.getTransaction().begin();
    ManagedEntityManagerContext.bind(em);
    keys.add(key);
    return em;
  }

  /**
   * Closes all JPA sessions opened for this request. If a transaction has
   * not been committed, it will be rolled back before closing the session.
   * @see net.databinder.components.jpa.DataForm#onSubmit()
   */
  @Override
  protected void onEndRequest() {
    for (final String key : keys) {
      final EntityManagerFactory emf = Databinder.getEntityManagerFactory(key);
      if (ManagedEntityManagerContext.hasBind(emf)) {
        closeEntityManager(key);
        ManagedEntityManagerContext.unbind(emf);
      }
    }
  }

  /**
   * Closes and reopens sessions for this request cycle. Unrelated models may
   * try to load themselves after this point.
   */
  @Override
  public Page onRuntimeException(final Page page, final RuntimeException e) {
    onEndRequest();
    onBeginRequest();
    return null;
  }

}
