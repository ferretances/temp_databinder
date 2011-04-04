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

package net.databinder.components.jpa;

import java.util.List;

import net.databinder.components.DataStyleLink;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.pages.AccessDeniedPage;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Page containing a QueryPanel for browsing data and testing JPA queries.
 * This page is not bookmarkable, so that it will not be inadverdantly available
 * from the classpath. To access the page it must be subclassed or manually
 * linked. DataApplication.BmarkDataBrowser is a subclass that requires that the
 * running application be assignable to DataApplication and return true for
 * isDataBrowserAllowed(). To use this page from an application that does not
 * extend DataApplication, make a bookmarkable subclass and call super(true), or
 * link to the class with PageLink.
 * @author Nathan Hamblen
 */
public class DataBrowser<T> extends WebPage {
  public DataBrowser(final boolean allowAccess) {
    if (allowAccess) {
      add(new DataStyleLink("css"));
      add(new QueryPanel("queryPanel"));
      add(new ListView<T>("entities", new LoadableDetachableModel<List<T>>() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        @Override
        protected List<T> load() {
          //          //TODO
          //          return new ArrayList<T>(Databinder.getEntityManager()
          //              .getEntityManagerFactory().getAllClassMetadata().keySet());
          return null;
        }
      }) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem item) {
          item.add(new Label("name", item.getModel()));
        }
      });

    } else {
      setResponsePage(AccessDeniedPage.class);
    }
  }
}