package net.databinder.models.jpa;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu

 * Databinder: a simple bridge from Wicket to JPA
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

import java.util.List;

import javax.persistence.criteria.Predicate;

import net.databinder.util.CriteriaDefinition;

import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Abstract base class for building OrderingPredicateBuilders. Uses an
 * ISortStateLocator to configure the sorting. Subclasses should call
 * super.buildUnordered() when overriding.
 * @author Mark Southern
 */
public abstract class PredicateBuildAndSort<T> extends
BasePredicateBuildAndSort<T> {

  private static final long serialVersionUID = 1L;

  public PredicateBuildAndSort(final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased,
      final CriteriaDefinition<T> criteriaDefinition) {
    super(defaultSortProperty, sortAscending, sortCased, criteriaDefinition);
  }

  @Override
  public void buildOrdered(final List<Predicate> criteria) {
    buildUnordered(criteria);

    final SortParam sort = ((SingleSortState) getSortState()).getSort();
    String property;
    if (sort != null && sort.getProperty() != null) {
      property = sort.getProperty();
      sortAscending = sort.isAscending();
    } else {
      property = defaultSortProperty;
    }

    if (property != null) {
      super.buildOrdered(criteria);
    }
  }

}