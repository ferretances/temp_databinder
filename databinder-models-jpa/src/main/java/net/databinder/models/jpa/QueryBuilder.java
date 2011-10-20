/*
 * Databinder: a simple bridge from Wicket to JPA
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

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
 */

package net.databinder.models.jpa;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.Query;


/**
 * Interface for callback that builds a {@link EntityManager} Query and binds it to parameters if necessary.
 * Use for SQL queries, named queries, etc.
 * @author Nathan Hamblen
 */
public interface QueryBuilder extends Serializable {
  /**
   * Create query from EntityManager and bind it to parameters.
   * @param entityManager entityManager for the current request cycle
   * @return ready-to-use query
   */
  Query build(EntityManager entityManager);
}
