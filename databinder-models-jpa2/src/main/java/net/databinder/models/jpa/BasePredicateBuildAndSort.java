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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import net.databinder.jpa.Databinder;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;

/**
 * Abstract base class for building OrderedCriteriaBuilders. It handles the
 * sorting. Subclasses should call super.buildUnordered() when overriding.
 * Avoids problems with duplicate Aliases by having all the Criteria building
 * code in one location.
 */
public abstract class BasePredicateBuildAndSort<T> implements
OrderingPredicateBuilder, Serializable, ISortStateLocator {

  private static final long serialVersionUID = 1L;

  protected Set<String> aliases = new HashSet<String>();

  protected String defaultSortProperty = null;

  protected boolean sortAscending, sortCased;

  protected Class<T> entityClass;

  private ISortState sortState = new SingleSortState();

  protected final Root<T> root ;
  protected final CriteriaQuery<Object> cq;
  protected final CriteriaBuilder cb;

  public BasePredicateBuildAndSort(final Class<T> entityClass) {
    this(null, true, false, entityClass);
  }

  public BasePredicateBuildAndSort(final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased,
      final Class<T> entityClass) {
    this.defaultSortProperty = defaultSortProperty;
    this.sortAscending = sortAscending;
    this.sortCased = sortCased;
    this.entityClass = entityClass;

    sortState.setPropertySortOrder(defaultSortProperty, ISortState.ASCENDING);
    setSortState(sortState);

    final EntityManager em = Databinder.getEntityManager();
    cb = em.getCriteriaBuilder();
    cq = cb.createQuery();
    root = cq.from(entityClass);
  }

  @Override
  public void buildOrdered(final List<Predicate> criteria) {
    buildUnordered(criteria);

    String property = defaultSortProperty;
    if (property != null) {
      cq.where(cb.and(criteria.toArray(new Predicate[0])));

      property = processProperty(criteria, property);
      cq.orderBy(sortAscending ? cb.asc(root.get(property)) : cb.desc(root
          .get(property)));
    }
  }

  @Override
  public void buildUnordered(final List<Predicate> criteria) {
    aliases.clear();
  }

  @Override
  public ISortState getSortState() {
    return sortState;
  }

  @Override
  public void setSortState(final ISortState state) {
    sortState = state;
  }

  protected String processProperty(final List<Predicate> criteria,
      String property) {
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
          // TODO
          // criteria.createAlias(sb.toString(), path[ii],
          // CriteriaSpecification.LEFT_JOIN);
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
