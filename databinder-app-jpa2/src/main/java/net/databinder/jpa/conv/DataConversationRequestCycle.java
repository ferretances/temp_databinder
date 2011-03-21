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

/*
 * Note: this class contains code adapted from wicket-contrib-database.
 */

package net.databinder.jpa.conv;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import net.databinder.jpa.DataRequestCycle;
import net.databinder.jpa.Databinder;
import net.databinder.jpa.ManagedEntityManagerContext;
import net.databinder.jpa.conv.components.IConversationPage;

import org.apache.wicket.Page;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports extended JPA sessions for long conversations. This is useful
 * for a page or a series of pages where changes are made to an entity that can
 * not be immediately committed. Using a "conversation" session,
 * JPAObjectModels are used normally, but until the session is flushed the
 * changes are not made to persistent storage.
 * @author Nathan Hamblen
 */
public class DataConversationRequestCycle extends DataRequestCycle {
  private static final Logger log = LoggerFactory
  .getLogger(DataConversationRequestCycle.class);

  public DataConversationRequestCycle(final WebApplication application,
      final WebRequest request, final Response response) {
    super(application, request, response);
  }

  /**
   * Does nothing; The session is open or retreived only when the request target
   * is known.
   */
  @Override
  protected void onBeginRequest() {
  }

  /**
   * Called by DataStaticService when a session is needed and does not already
   * exist. Determines current page and retrieves its associated conversation
   * session if appropriate. Does nothing if current page is not yet available.
   * @param key factory key object, or null for the default factory
   */
  public void dataEntityMangerRequested(final String key) {
    Page page = getResponsePage();
    if (page == null) {
      page = getRequest().getPage();
    }

    if (page == null) {
      final Class<?> pageClass = getResponsePageClass();
      if (pageClass != null) {
        openEntityManager(key);
        // set to manual if we are going to a conv. page
        if (IConversationPage.class.isAssignableFrom(pageClass)) {
          Databinder.getEntityManager(key).setFlushMode(FlushModeType.AUTO);
        }
      }
      return;
    }

    // if continuing a conversation page
    if (page instanceof IConversationPage) {
      // look for existing session
      final IConversationPage convPage = (IConversationPage) page;
      EntityManager em = convPage.getConversationEntityManger(key);

      // if usable session exists, try to open txn, bind, and return
      if (em != null && em.isOpen()) {
        try {
          em.getTransaction().begin();
          ManagedEntityManagerContext.bind(em);
          keys.add(key);
          return;
        } catch (final PersistenceException e) {
          log.warn(
              "Existing em exception on beginTransation, opening new", e);
        }
      }
      // else start new one and set in page
      em = openEntityManager(key);
      em.setFlushMode(FlushModeType.COMMIT);
      ((IConversationPage) page).setConversationEntityManager(key, em);
      return;
    }
    // start new standard session
    openEntityManager(key);
  }

  /**
   * Inspects responding page to determine if current JPA em should
   * be closed or left open and stored in the page.
   */
  @Override
  protected void onEndRequest() {
    for (final String key : keys) {
      if (!ManagedEntityManagerContext.hasBind(Databinder
          .getEntityManagerFactory())) {
        return;
      }
      EntityManager em = Databinder.getEntityManager(key);
      boolean transactionComitted = false;
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      } else {
        transactionComitted = true;
      }

      final Page page = getResponsePage();

      if (page != null) {
        // check for current conversational session
        if (page instanceof IConversationPage) {
          final IConversationPage convPage = (IConversationPage) page;
          // close if not dirty contains no changes
          // TODO: if (transactionComitted && !em.Dirty()) {
          if (transactionComitted) {
            em.close();
            em = null;
          }
          convPage.setConversationEntityManager(key, em);
        } else {
          em.close();
        }
      }
      ManagedEntityManagerContext.unbind(Databinder.getEntityManager(key).getEntityManagerFactory());
    }
  }

  /**
   * Closes and reopens Hibernate session for this Web session. Unrelated models
   * may try to load themselves after this point.
   */
  @Override
  public Page onRuntimeException(final Page page, final RuntimeException e) {
    for (final String key : keys) {
      if (Databinder.hasEntityManagerBound(key)) {
        final EntityManager sess = Databinder.getEntityManager(key);
        try {
          if (sess.getTransaction().isActive()) {
            sess.getTransaction().rollback();
          }
        } finally {
          sess.close();
          ManagedEntityManagerContext.unbind(Databinder.getEntityManager(key)
              .getEntityManagerFactory());
        }
      }
      openEntityManager(key);
    }
    return null;
  }

}
