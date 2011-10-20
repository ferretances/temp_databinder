/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2007
 * Nathan Hamblen nathan@technically.us Copyright (C) 2007 xoocode.org project
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version. This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details. You should have received a copy of
 * the GNU Lesser General Public License along with this library; if not, write
 * to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 * MA 02110-1301 USA
 */
package net.databinder.components.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type.PersistenceType;

import net.databinder.jpa.Databinder;
import net.databinder.models.jpa.JPAObjectModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

/**
 * A Panel used to display a textarea to enter an JPAQL query and execute it
 * against the current entity manager of a {@link EntityManagerFactory}.
 * <p>
 * The panel result is displayed in a data table, where columns are created
 * according to the query.
 * <p>
 * For instance, a query like:
 * 
 * <pre>
 * select job.name as name, job.id as id from JobModel job
 * </pre>
 * 
 * will result in two columns, 'name' and 'id', in the result data table.
 * <p>
 * If you run:
 * 
 * <pre>
 * from JobModel
 * </pre>
 * 
 * the columns in the result table will be the available properties of a
 * JobModel
 */
public class QueryPanel extends Panel {
  private static final long serialVersionUID = 1L;

  /**
   * Bean used to store the query
   */
  private final QueryBean query = new QueryBean();
  /**
   * Stores information about the query execution (executed query, time, ...)
   */
  private String executionInfo;

  /**
   * Constructs an {@link QueryPanel}
   * @param id the panel identifier. Must not be null.
   */
  public QueryPanel(final String id) {
    super(id);

    final WebMarkupContainer resultsHolder =
      new WebMarkupContainer("resultsHolder");
    resultsHolder.add(new Label("executionInfo", new PropertyModel(this,
    "executionInfo")));
    resultsHolder.add(getResultsTable());
    resultsHolder.setOutputMarkupId(true);
    add(resultsHolder);

    final Form<QueryBean> form =
      new Form<QueryBean>("form", new CompoundPropertyModel<QueryBean>(query));
    form.setOutputMarkupId(true);
    form.add(new TextArea("query"));
    form.add(new AjaxButton("submit", form) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form form) {
        if (resultsHolder.get("results") != null) {
          resultsHolder.remove("results");
        }
        try {
          resultsHolder.add(getResultsTable());
        } catch (final QueryTimeoutException e) {
          note(e);
        } catch (final IllegalArgumentException e) {
          note(e);
        } catch (final IllegalStateException e) {
          note(e);
        }
        target.addComponent(resultsHolder);
      }

      private void note(final Exception e) {
        resultsHolder.add(new Label("results", e.getClass().getSimpleName()
            + ": " + e.getMessage()));
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form<?> form) {
      }
    });
    add(form);
  }

  /**
   * Creates a result table for the current query.
   * @return a result table, or an empty label if there is no current query
   */
  @SuppressWarnings("unchecked")
  private Component getResultsTable() {
    if (Strings.isEmpty(query.getQuery())) {
      return new Label("results", "");
    } else {
      final IDataProvider dataProvider = new IDataProvider() {
        private static final long serialVersionUID = 1L;

        public void detach() {
        }

        public int size() {
          final EntityManager sess = Databinder.getEntityManager();
          final Query query = sess.createQuery(getQuery());
          return query.getResultList().size();
        }

        public String getQuery() {
          return query.getQuery();
        }

        @SuppressWarnings("unchecked")
        public IModel<?> model(final Object object) {
          return new CompoundPropertyModel(new JPAObjectModel(object));
        }

        public Iterator iterator(final int first, final int count) {
          final EntityManager em = Databinder.getEntityManager();
          final long start = System.nanoTime();
          try {
            final Query q = em.createQuery(getQuery());
            q.setFirstResult(first);
            q.setMaxResults(count);
            return q.getResultList().iterator();
          } finally {
            final float nanoTime = (System.nanoTime() - start) / 1000 / 1000.0f;
            setExecutionInfo("query executed in " + nanoTime + " ms: "
                + getQuery());
          }
        }
      };
      IColumn[] columns;
      final EntityManager em = Databinder.getEntityManager();
      final Query q = em.createQuery(query.getQuery());
      final String[] aliases = null;
      final javax.persistence.metamodel.Type<?>[] returnTypes = null;
      try {
        //         aliases = q.getReturnAliases();
        //        returnTypes = q.getReturnTypes();
      } catch (final NullPointerException e) { // thrown on updates
        return new Label("results", "");
      }

      if (returnTypes.length != 1) {
        columns = new IColumn[returnTypes.length];
        for (int i = 0; i < returnTypes.length; i++) {
          final String alias =
            aliases == null || aliases.length <= i ? returnTypes[i]
                                                                 .getJavaType().getName() : aliases[i];
                                                                 final int index = i;
                                                                 columns[i] = new AbstractColumn(new Model(alias)) {
                                                                   private static final long serialVersionUID = 1L;

                                                                   public void populateItem(final Item cellItem,
                                                                       final String componentId, final IModel rowModel) {
                                                                     final Object[] objects = (Object[]) rowModel.getObject();
                                                                     cellItem.add(new Label(componentId, new Model(
                                                                         objects[index] == null ? "" : objects[index].toString())));
                                                                   }
                                                                 };
        }
      } else {
        final javax.persistence.metamodel.Type<?> returnType = returnTypes[0];
        if (returnType.getJavaType().isInterface()) {
          final PersistenceType persistenceType =
            returnType.getPersistenceType();
          final Class<?> clss = persistenceType.getDeclaringClass();
          final EntityType<?> entityType =
            Databinder.getEntityManagerFactory().getMetamodel().entity(clss);
          final List<IColumn> cols = new ArrayList<IColumn>();
          final String idProp = entityType.getIdType().toString();
          cols.add(new PropertyColumn(new Model(idProp), idProp));
          final Set<?> properties = entityType.getAttributes();
          for (final Object prop : properties) {
            final Attribute type = entityType.getAttribute(prop.toString());
            if (type.isCollection()) {
              // TODO: see if we could provide a link to the collection value
            } else {
              cols.add(new PropertyColumn<String>(new Model<String>(prop
                  .toString()), prop.toString()));
            }
          }
          columns = cols.toArray(new IColumn[cols.size()]);
        } else {
          final String alias =
            aliases == null || aliases.length == 0 ? returnType.getJavaType()
                .getName() : aliases[0];
                columns = new IColumn[] { new AbstractColumn(new Model(alias)) {
                  private static final long serialVersionUID = 1L;

                  public void populateItem(final Item cellItem,
                      final String componentId, final IModel rowModel) {
                    cellItem.add(new Label(componentId, rowModel));
                  }
                } };
        }
      }
      final List cols = Arrays.asList(columns);
      final DataTable dataTable =
          new DataTable("results", cols, dataProvider, 10);

      dataTable.addTopToolbar(new HeadersToolbar(dataTable, null));
      dataTable.addBottomToolbar(new NavigationToolbar(dataTable));
      dataTable.setOutputMarkupId(true);
      return dataTable;
    }
  }

  private static class QueryBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private String query;

    public String getQuery() {
      return query;
    }

    public void setQuery(final String query) {
      this.query = query;
    }
  }

  public String getExecutionInfo() {
    return executionInfo;
  }

  public void setExecutionInfo(final String executionInfo) {
    this.executionInfo = executionInfo;
  }

}
