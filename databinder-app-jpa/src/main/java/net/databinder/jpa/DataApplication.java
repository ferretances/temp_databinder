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

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import net.databinder.DataApplicationBase;
import net.databinder.components.jpa.DataBrowser;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;

/**
 * Optional Databinder base Application class for configuration and session
 * management. Supports multiple session factories with key objects.
 * @author Nathan Hamblen
 */
public abstract class DataApplication extends DataApplicationBase implements
JPAApplication {

  /** App-wide EntityManager factories */
  private final HashMap<Object, EntityManagerFactory> entityManagerFactories =
    new HashMap<Object, EntityManagerFactory>();

  /**
   * Initializes a default JPA EntityManager factory and mounts a page for the
   * data browser. This is called automatically during start-up. Applications
   * with one EntityManager factory will not normally need to override this method;
   * see related methods to override specific tasks.
   * @see #buildEntityManagerFactory(Object) aoe
   * @see #mountDataBrowser()
   */
  @Override
  protected void dataInit() {
    buildEntityManagerFactory(null, configureEMF());
    if (isDataBrowserAllowed()) {
      mountDataBrowser();
    }
  }

  /**
   * Bookmarkable subclass of DataBrowser page. Access to the page is permitted
   * only if the current application is assignable to DataApplication and
   * returns true for isDataBrowserAllowed().
   * @see DataBrowser
   */
  public static class BmarkDataBrowser extends DataBrowser {
    public BmarkDataBrowser() {
      super(((DataApplication) Application.get()).isDataBrowserAllowed());
    }
  }

  /**
   * Mounts Data Diver to /dbrowse. Override to mount elsewhere, or not mount at
   * all. This method is only called if isDataBrowserAllowed() returns true in
   * init().
   */
  protected void mountDataBrowser() {
	  mountPage("/dbrowse", BmarkDataBrowser.class);
  }

  /**
   * @param key object, or null for the default factory
   * @return the retained EntityManager factory
   */
  public EntityManagerFactory getJPAEntityManagerFactory(final Object key) {
    final EntityManagerFactory sf = entityManagerFactories.get(key);
    if (sf == null) {
      if (key == null) {
        throw new WicketRuntimeException(
            "The default JPA emf has not been "
            + "initialized. This is normally done in DataApplication.init().");
      } else {
        throw new WicketRuntimeException("EntityManager factory not found for key: "
            + key);
      }
    }
    return sf;
  }

  /**
   * @param key object, or null for the default factory w
   * @param sf EntityManager factory to retain
   */
  protected void setEntityManagerFactory(final Object key,
      final EntityManagerFactory sf) {
    entityManagerFactories.put(key, sf);
  }

  /**
   * Returns true if development mode is enabled. Override for other behavior.
   * @return true if the Data Browser page should be enabled
   */
  protected boolean isDataBrowserAllowed() {
    return isDevelopment();
  }

  /**
   * Called by init to create JPA EntityManager factory and load a
   * configuration. Passes an empty new AnnotationConfiguration to
   * buildJPAEntityManagerFactory(key, config) by default. Override if creating
   * a configuration externally.
   * @param key EntityManager factory key; the default key is null
   */
  public void buildEntityManagerFactory(final Object key, final EntityManagerFactory em) {
    setEntityManagerFactory(key, em);
  }

  public abstract EntityManagerFactory configureEMF() ;
}
