package net.databinder.jpa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

public class DefaultEntityManagerContext implements Serializable,
EntityManagerContext {

  private static final long serialVersionUID = 1L;

  private static final ThreadLocal<Map<Object, EntityManager>> context =
    new ThreadLocal<Map<Object, EntityManager>>();

  private final EntityManagerFactory factory;

  public DefaultEntityManagerContext(final EntityManagerFactory factory) {
    this.factory = factory;
  }

  /**
   * @see net.databinder.jpa.EntityManagerContext#currentEntityManager()
   */
  public EntityManager currentEntityManager() {
    final EntityManager current = existingEntityManager(factory);
    if (current == null) {
      throw new PersistenceException(
      "No EntityManger currently bound to execution context");
    }
    return current;
  }

  /**
   * Check to see if there is already a entity manager associated with the
   * current thread for the given entity manager factory.
   * @return True if there is currently a entity manager bound.
   */
  public boolean hasBind() {
    return existingEntityManager(factory) != null;
  }

  /**
   * Binds the given entity manager to the current context for its entity
   * manager factory.
   * @param em The entity manager to be bound.
   * @return Any previously bound entity manager (should be null in most cases).
   */
  public EntityManager bind(final EntityManager em) {
    return entityManagerMap(true).put(em.getEntityManagerFactory(), em);
  }

  /**
   * Unbinds the entity manager (if one) current associated with the context for
   * the given entity manager.
   * @return The bound entity manager if one, else null.
   */
  public EntityManager unbind() {
    EntityManager existing = null;
    final Map<Object, EntityManager> emMap = entityManagerMap();
    if (emMap != null) {
      existing = emMap.remove(factory);
      doCleanup();
    }
    return existing;
  }

  private static EntityManager existingEntityManager(
      final EntityManagerFactory emf) {
    final Map<Object, EntityManager> emMap = entityManagerMap();
    if (emMap == null) {
      return null;
    } else {
      return emMap.get(emf);
    }
  }

  protected static Map<Object, EntityManager> entityManagerMap() {
    return entityManagerMap(false);
  }

  private static synchronized Map<Object, EntityManager> entityManagerMap(
      final boolean createMap) {
    Map<Object, EntityManager> emMap = context.get();
    if (emMap == null && createMap) {
      emMap = new HashMap<Object, EntityManager>();
      context.set(emMap);
    }
    return emMap;
  }

  private static synchronized void doCleanup() {
    final Map<Object, EntityManager> emMap = entityManagerMap(false);
    if (emMap != null) {
      if (emMap.isEmpty()) {
        context.set(null);
      }
    }
  }

}
