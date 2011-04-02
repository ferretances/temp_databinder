package net.databinder.models.jpa;

import javax.persistence.Query;

import org.apache.wicket.util.lang.PropertyResolver;

/**
 * Base class for classes that bind queries using object properties.
 * @author Jonathan
 */
public abstract class AbstractPropertyQueryBinder implements QueryBinder {

  private static final long serialVersionUID = 1L;

  /**
   * @param query The query to bind
   * @param object The object to pull properties from
   * @param parameters
   */
  protected void bind(final Query query, final Object object,
      final String[] parameters) {
    for (final String parameter : parameters) {
      query.setParameter(parameter,
          PropertyResolver.getValue(parameter, object));
    }
  }
}
