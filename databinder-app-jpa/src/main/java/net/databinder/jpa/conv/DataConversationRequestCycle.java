/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2006 Nathan
 * Hamblen nathan@technically.us This library is free software; you can
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
import net.databinder.jpa.EntityManagerContext;
import net.databinder.jpa.conv.components.IConversationPage;

import org.apache.wicket.Page;
import org.apache.wicket.request.cycle.RequestCycleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports extended JPA entity managers for long conversations. This is useful
 * for a page or a series of pages where changes are made to an entity that can
 * not be immediately committed. Using a "conversation" session, JPAObjectModels
 * are used normally, but until the session is flushed the changes are not made
 * to persistent storage.
 *
 * @author Nathan Hamblen
 */
public class DataConversationRequestCycle extends DataRequestCycle {
	private static final Logger log = LoggerFactory
			.getLogger(DataConversationRequestCycle.class);

	public DataConversationRequestCycle(
			final RequestCycleContext requestCycleContext) {
		super(requestCycleContext);
	}

	/**
	 * Does nothing; The EntityManager is open or retreived only when the
	 * request target is known.
	 */
	@Override
	protected void onBeginRequest() {
	}

	/**
	 * Called by DataStaticService when a EntityManager is needed and does not
	 * already exist. Determines current page and retrieves its associated
	 * conversation EntityManager if appropriate. Does nothing if current page
	 * is not yet available.
	 *
	 * @param key
	 *            factory key object, or null for the default factory
	 */
	public void dataEntityMangerRequested(final String key) {
		Page page = getResponsePage();
		if (page == null) {
			page = getRequestPage();
		}

		if (page == null) {
			final Class<?> pageClass = getResponsePageClass();
			if (pageClass != null) {
				openEntityManager(key);
				// set to manual if we are going to a conv. page
				if (IConversationPage.class.isAssignableFrom(pageClass)) {
					Databinder.getEntityManager(key).setFlushMode(
							FlushModeType.AUTO);
				}
			}
			return;
		}

		// if continuing a conversation page
		if (page instanceof IConversationPage) {
			// look for existing EntityManager
			final IConversationPage convPage = (IConversationPage) page;
			EntityManager em = convPage.getConversationEntityManger(key);

			// if usable EntityManager exists, try to open txn, bind, and return
			if (em != null && em.isOpen()) {
				try {
					em.getTransaction().begin();
					Databinder.getEntityManagerContext(key).bind(em);
					keys.add(key);
					return;
				} catch (final PersistenceException e) {
					log.warn(
							"Existing em exception on beginTransation, opening new",
							e);
				}
			}
			// else start new one and set in page
			em = openEntityManager(key);
			em.setFlushMode(FlushModeType.COMMIT);
			((IConversationPage) page).setConversationEntityManager(key, em);
			return;
		}
		// start new standard EntityManager
		openEntityManager(key);
	}

	/**
	 * Inspects responding page to determine if current JPA em should be closed
	 * or left open and stored in the page.
	 */
	@Override
	protected void onEndRequest() {
		for (final String key : keys) {
			final EntityManagerContext emc = Databinder
					.getEntityManagerContext(key);
			if (!emc.hasBind()) {
				return;
			}
			final EntityManager em = emc.currentEntityManager();
			boolean transactionComitted = false;
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			} else {
				transactionComitted = true;
			}

			final Page page = getResponsePage();

			if (page != null) {
				// check for current conversational EntityManager
				if (page instanceof IConversationPage) {
					final IConversationPage convPage = (IConversationPage) page;
					// close if not dirty contains no changes
					// TODO: if (transactionComitted && !em.Dirty()) {
					// em.close();
					// em = null;
					// }
					convPage.setConversationEntityManager(key, em);
				} else {
					em.close();
				}
			}
			emc.unbind();
		}
	}

	/**
	 * Closes and reopens JPA entity manager for this Web EntityManager.
	 * Unrelated models may try to load themselves after this point.
	 */
	public Page onRuntimeException(final Page page, final RuntimeException e) {
		for (final String key : keys) {
			final EntityManagerContext emc = Databinder
					.getEntityManagerContext(key);
			if (emc.hasBind()) {
				final EntityManager em = emc.currentEntityManager();
				try {
					if (em.getTransaction().isActive()) {
						em.getTransaction().rollback();
					}
				} finally {
					em.close();
					emc.unbind();
				}
			}
			openEntityManager(key);
		}
		return null;
	}

	private Page getResponsePage() {
		// TODO Auto-generated method stub
		return null;
	}

	private Page getRequestPage() {
		// TODO Auto-generated method stub
		// getRequest().getPage()
		return null;
	}

	private Class getResponsePageClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
