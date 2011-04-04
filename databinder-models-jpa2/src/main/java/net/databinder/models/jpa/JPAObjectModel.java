/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2006 Nathan
 * Hamblen nathan@technically.us This library is free software; you can
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Version;
import javax.persistence.criteria.Predicate;

import net.databinder.jpa.Databinder;
import net.databinder.models.BindingModel;
import net.databinder.models.LoadableWritableModel;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IChainingModel;

/**
 * Model loaded and persisted by JPA. This central Databinder class can be
 * initialized with an entity ID, different types of queries, or an existing
 * persistent object. As a writable Wicket model, the object it contains may be
 * swapped at any time for a different persistent object, a Serializable object,
 * or null.
 * @author Nathan Hamblen
 */
public class JPAObjectModel<T> extends LoadableWritableModel<T> implements
BindingModel<T> {

  private static final long serialVersionUID = 1L;

  private Class<T> entityClass;
  private Object objectId;
  private QueryBuilder queryBuilder;
  private PredicateBuilder<?> criteriaBuilder;

  /** May store unsaved objects between requests. */
  private T retainedObject;
  /** Enable retaining unsaved objects between requests. */
  private boolean retainUnsaved = true;

  private String factoryKey = Databinder.DEFAULT_PERSISTENCE_UNIT_NAME;

  /**
   * Create a model bound to the given class and entity id. If nothing matches
   * the id the model object will be null.
   * @param objectClass class to be loaded and stored by JPA
   * @param entityId id of the persistent object
   */
  public JPAObjectModel(final Class<T> objectClass, final Serializable entityId) {
    this.entityClass = objectClass;
    this.objectId = entityId;
  }

  /**
   * Constructor for a model with no existing persistent object. This class
   * should be Serializable so that the new object can be stored in the
   * EntityManager until it is persisted. If serialization is impossible, call
   * setRetainUnsaved(false) and the object will be discarded and recreated with
   * each request.
   * @param objectClass class to be loaded and stored by JPA
   */
  public JPAObjectModel(final Class<T> objectClass) {
    this.entityClass = objectClass;
  }

  /**
   * Construct with an entity.
   * @param persistentObject should be previously persisted or Serializable for
   *          temp storage.
   */
  public JPAObjectModel(final T persistentObject) {
    setObject(persistentObject);
  }

  /**
   * Construct with a query and binder that return exactly one result. Use this
   * for fetch instructions, scalar results, or if the persistent object ID is
   * not available. Queries that return more than one result will produce
   * exceptions. Queries that return no result will produce a null object.
   * @param queryString query returning one result
   * @param queryBinder bind id or other parameters
   */
  public JPAObjectModel(final String queryString, final QueryBinder queryBinder) {
    this(new QueryBinderBuilder(queryString, queryBinder));
  }

  /**
   * Construct with a class and predicate binder that return exactly one result.
   * Use this for fetch instructions, scalar results, or if the persistent
   * object ID is not available. Predicate that return more than one result will
   * produce exceptions. Criteria that return no result will produce a null
   * object.
   * @param objectClass class of object for root criteria
   * @param criteriaBuilder builder to apply criteria restrictions
   */
  public JPAObjectModel(final Class<T> objectClass,
      final PredicateBuilder<T> criteriaBuilder) {
    this.entityClass = objectClass;
    this.criteriaBuilder = criteriaBuilder;
  }

  /**
   * Construct with a query builder that returns exactly one result, used for
   * custom query objects. Queries that return more than one result will produce
   * exceptions. Queries that return no result will produce a null object.
   * @param queryBuilder builder to create and bind query object
   */
  public JPAObjectModel(final QueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
  }

  /**
   * Construct with no object. Will return null for getObject().
   */
  public JPAObjectModel() {
  }

  /** @return entity manager factory key, or null for the default factory */
  public String getFactoryKey() {
    return factoryKey;
  }

  /**
   * Set a factory key other than the default (null).
   * @param key EntityManager factory key
   * @return this, for chaining
   */
  public JPAObjectModel<T> setFactoryKey(final String key) {
    this.factoryKey = key;
    return this;
  }

  /**
   * Change the persistent object contained in this model. Because this method
   * establishes a persistent object ID, queries and binders are removed if
   * present.
   * @param object must be an entity contained in the current JPA
   *          {@link EntityManager}, or Serializable, or null
   */
  @SuppressWarnings("unchecked")
  public void setObject(final T object) {
    unbind(); // clear everything but class, name
    entityClass = null;

    if (object != null) {
      T obj = object;
      if (object instanceof IChainingModel<?>) {
        obj = (T) ((IChainingModel<?>) object).getObject();
      }
      entityClass = (Class<T>) obj.getClass();
      final EntityManager em = Databinder.getEntityManager(factoryKey);
      if (em.contains(obj)) {
        objectId =
          em.getEntityManagerFactory().getPersistenceUnitUtil()
          .getIdentifier(obj);
      } else if (retainUnsaved) {
        retainedObject = obj;
      }
      setTempModelObject(obj); // skip calling load later
    }

  }

  public Object getIdentifier() {
    final EntityManager em = Databinder.getEntityManager();
    final EntityManagerFactory emf = em.getEntityManagerFactory();
    final PersistenceUnitUtil pu = emf.getPersistenceUnitUtil();
    return pu.getIdentifier(getObject());
  }

  /**
   * Load the object through JPA, contruct a new instance if it is not bound to
   * an id, or use unsaved retained object. Returns null if no criteria needed
   * to load or construct an object are available.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected T load() {
    if (entityClass == null && queryBuilder == null) {
      return null; // can't load without one of these
    }
    try {
      if (!isBound()) {
        if (retainUnsaved && retainedObject != null) {
          return retainedObject;
        } else if (retainUnsaved) {
          try {
            return retainedObject = entityClass.newInstance();
          } catch (final ClassCastException e) {
            throw new WicketRuntimeException(
            "Unsaved entity must be Serializable or retainUnsaved set to false; see JPAObjectModel javadocs.");
          }
        } else {
          return entityClass.newInstance();
        }
      }
    } catch (final ClassCastException e) {
      throw new RuntimeException(
          "Retaining unsaved model objects requires that they be Serializable.",
          e);
    } catch (final Throwable e) {
      throw new RuntimeException(
          "Unable to instantiate object. Does it have a default constructor?",
          e);
    }
    final EntityManager em = Databinder.getEntityManager(factoryKey);
    if (objectId != null) {
      return em.getReference(entityClass, objectId);
    }

    if (criteriaBuilder != null) {
      final javax.persistence.criteria.CriteriaBuilder cb =
        em.getCriteriaBuilder();
      criteriaBuilder.build(new ArrayList<Predicate>());
      return (T) em.createQuery(cb.createQuery()).getSingleResult();
    }

    return (T) queryBuilder.build(em).getSingleResult();
  }

  /**
   * Checks if the model is retaining an object this has since become a
   * persistent entity. If so, the ID is fetched and the reference discarded.
   */
  public void checkBinding() {
    if (!isBound() && retainedObject != null) {
      final EntityManager em = Databinder.getEntityManager(factoryKey);

      if (em.contains(retainedObject)) {
        objectId =
          em.getEntityManagerFactory().getPersistenceUnitUtil()
          .getIdentifier(retainedObject);
        retainedObject = null;
      }
    }
  }

  /**
   * Uses version annotation to find version for this Model's object.
   * @return Persistent storage version number if available, null otherwise
   */
  public Serializable getVersion() {
    final Object o = getObject();

    if (o != null) {
      @SuppressWarnings("unchecked")
      final Class<T> c = (Class<T>) o.getClass();
      try {
        for (final Method m : c.getMethods()) {
          if (m.isAnnotationPresent(Version.class)
              && m.getParameterTypes().length == 0
              && m.getReturnType() instanceof Serializable) {
            return (Serializable) m.invoke(o, new Object[] {});
          }
        }
        for (final Field f : c.getDeclaredFields()) {
          if (f.isAnnotationPresent(Version.class)
              && f.getType() instanceof Serializable) {
            f.setAccessible(true);
            return (Serializable) f.get(o);
          }
        }
      } catch (final Exception e) {
        throw new PersistenceException(e);
      }
    }
    return null;
  }

  /**
   * Compares contained objects if present, otherwise calls
   * super-implementation.
   */
  @Override
  public boolean equals(final Object obj) {
    final Object target = getObject();
    if (target != null && obj instanceof JPAObjectModel) {
      return target.equals(((JPAObjectModel<T>) obj).getObject());
    }
    return super.equals(obj);
  }

  /**
   * @return hash of contained object if present, otherwise from
   *         super-implementation.
   */
  @Override
  public int hashCode() {
    final Object target = getObject();
    if (target == null) {
      return super.hashCode();
    }
    return target.hashCode();
  }

  /**
   * Disassociates this object from any persistent object, but retains the class
   * for constructing a blank copy if requested.
   * @see JPAObjectModel#JPAObjectModel(Class objectClass)
   * @see #isBound()
   */
  public void unbind() {
    objectId = null;
    queryBuilder = null;
    criteriaBuilder = null;
    retainedObject = null;
    detach();
  }

  /**
   * "bound" models are those that can be loaded from persistent storage by a
   * known id or query. When bound, this model discards its temporary model
   * object at the end of every request cycle and reloads it via Hiberanate when
   * needed again. When unbound, its behavior is dictated by the value of
   * retanUnsaved.
   * @return true if information needed to load from JPA (identifier, query, or
   *         criteria) is present
   */
  public boolean isBound() {
    return objectId != null || criteriaBuilder != null || queryBuilder != null;
  }

  /**
   * When retainUnsaved is true (the default) and the model is not bound, the
   * model object must be Serializable as it is retained in the Web
   * EntityManager between requests. See isBound() for more information.
   * @return true if unsaved objects should be retained between requests.
   */
  public boolean getRetainUnsaved() {
    return retainUnsaved;
  }

  /**
   * Unsaved Serializable objects can be retained between requests.
   * @param retainUnsaved set to true to retain unsaved objects
   */
  public void setRetainUnsaved(final boolean retainUnsaved) {
    this.retainUnsaved = retainUnsaved;
  }
}
