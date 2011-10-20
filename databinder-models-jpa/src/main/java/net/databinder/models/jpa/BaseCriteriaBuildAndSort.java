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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Abstract base class for building OrderedCriteriaBuilders. It handles the
 * sorting. Subclasses should call super.buildUnordered() when overriding.
 * Avoids problems with duplicate Aliases by having all the Criteria building
 * code in one location.
 */
public abstract class BaseCriteriaBuildAndSort<T> implements
OrderingCriteriaBuilder, Serializable {

  private static final long serialVersionUID = 1L;

  protected Set<String> aliases = new HashSet<String>();

  protected String defaultSortProperty = null;

  protected boolean sortAscending, sortCased;

  protected Class<T> entityClass;

  public BaseCriteriaBuildAndSort(final Class<T> entityClass) {
    this(null, true, false, entityClass);
  }

  public BaseCriteriaBuildAndSort(final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased, final Class<T> entityClass) {
    this.defaultSortProperty = defaultSortProperty;
    this.sortAscending = sortAscending;
    this.sortCased = sortCased;
    this.entityClass = entityClass;
  }

  public void buildOrdered(final javax.persistence.criteria.CriteriaBuilder criteria) {
    buildUnordered(criteria);

    String property = defaultSortProperty;
    if (property != null) {
      property = processProperty(criteria, property);
      final CriteriaQuery<T> query = criteria.createQuery(entityClass);
      final Root<T> root = criteria.createQuery().from(entityClass);
      query.orderBy(sortAscending ? criteria.asc(root.get(property)) : criteria.desc(root.get(property)));
    }
  }

  public void buildUnordered(final javax.persistence.criteria.CriteriaBuilder criteria) {
    aliases.clear();
  }

  protected String processProperty(final javax.persistence.criteria.CriteriaBuilder criteria, String property) {
    if (property.contains(".")) {
      // for 'dot' properties we need to add aliases
      // e.g. for the property 'orderbook.order.item.name' we need to add an
      // aliases for 'order' and 'order.item'
      final String path[] = property.split("\\.");
      for (int ii = 0; ii < path.length - 1; ii++) {
        final StringBuffer sb = new StringBuffer();
        for (int jj = 0; jj <= ii; jj++) {
          if (sb.length() > 0) {
            sb.append(".");
          }
          sb.append(path[jj]);
        }
        if (!aliases.contains(path[ii])) {
          aliases.add(path[ii]);
          //TODO
          //          criteria.createAlias(sb.toString(), path[ii],
          //              CriteriaSpecification.LEFT_JOIN);
        }
      }
      // when we have a 'dot' property we want to sort by the sub tables field
      // e.g. for the property 'orderbook.order.item.name' we need to sort by
      // 'item.name'
      if (path.length > 1) {
        property =
          String
          .format("%s.%s", path[path.length - 2], path[path.length - 1]);
      } else {
        property = path[path.length - 1];
      }
    }
    return property;
  }
}
