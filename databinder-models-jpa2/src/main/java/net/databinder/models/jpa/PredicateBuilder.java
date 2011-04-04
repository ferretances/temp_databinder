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

package net.databinder.models.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.Predicate;

import net.databinder.util.CriteriaDefinition;

/**
 * Interface for callback that sets parameters for a {@link Predicate} object
 * and any necessary sub-criteria.
 * @author Nathan Hamblen
 */
public interface PredicateBuilder<T> extends Serializable {

  /**
   * @param criteriaDefinition
   * @return {@link PredicateBuilder}
   */
  PredicateBuilder<T> setCriteriaDefinition(
      final CriteriaDefinition<T> criteriaDefinition);

  /**
   * @return {@link CriteriaDefinition}
   */
  CriteriaDefinition<T> getCriteriaDefinition();

  /** Add predicates to criteria definition */
  void build(final List<Predicate> predicates);
}
