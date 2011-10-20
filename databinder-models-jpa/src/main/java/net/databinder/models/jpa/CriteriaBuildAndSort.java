package net.databinder.models.jpa;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu

 * Databinder: a simple bridge from Wicket to Hibernate
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 ---*/

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Abstract base class for building OrderedCriteriaBuilders. Uses an
 * ISortStateLocator to configure the sorting. Subclasses should call
 * super.buildUnordered() when overriding.
 * @author Mark Southern
 */
public abstract class CriteriaBuildAndSort<T> extends
BaseCriteriaBuildAndSort<T> implements ISortStateLocator {

  private static final long serialVersionUID = 1L;

  private SingleSortState sortState = new SingleSortState();

  public CriteriaBuildAndSort(final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased,
      final Class<T> entityClass) {
    super(defaultSortProperty, sortAscending, sortCased, entityClass);
  }

  @Override
  public void buildOrdered(
      final javax.persistence.criteria.CriteriaBuilder criteria) {
    buildUnordered(criteria);

    final SortParam sort = sortState.getSort();
    String property;
    if (sort != null && sort.getProperty() != null) {
      property = sort.getProperty();
      sortAscending = sort.isAscending();
    } else {
      property = defaultSortProperty;
    }

    if (property != null) {
      property = processProperty(criteria, property);
      final CriteriaQuery<T> query = criteria.createQuery(entityClass);
      final Root<T> root = criteria.createQuery().from(entityClass);
      query.orderBy(sortAscending ? criteria.asc(root.get(property)) : criteria
          .desc(root.get(property)));
    }
  }

  public ISortState getSortState() {
    return sortState;
  }

  public void setSortState(final ISortState state) {
    sortState = (SingleSortState) state;
  }
}