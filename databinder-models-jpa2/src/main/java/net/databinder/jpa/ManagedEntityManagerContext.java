package net.databinder.jpa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

public class ManagedEntityManagerContext implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final ThreadLocal<Map<Object, EntityManager>> context =
    new ThreadLocal<Map<Object, EntityManager>>();

  private final EntityManagerFactory factory;

  public ManagedEntityManagerContext(final EntityManagerFactory factory) {
    this.factory = factory;
  }

  /**
   * {@inheritDoc}
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
   * Check to see if there is already a session associated with the current
   * thread for the given session factory.
   * @param emf The factory against which to check for a given session within
   *          the current thread.
   * @return True if there is currently a session bound.
   */
  public static boolean hasBind(final EntityManagerFactory emf) {
    return existingEntityManager(emf) != null;
  }

  /**
   * Binds the given session to the current context for its session factory.
   * @param em The session to be bound.
   * @return Any previously bound session (should be null in most cases).
   */
  public static EntityManager bind(final EntityManager em) {
    return entityManagerMap(true).put(
        em.getEntityManagerFactory(), em);
  }

  /**
   * Unbinds the session (if one) current associated with the context for the
   * given session.
   * @param factory The factory for which to unbind the current session.
   * @return The bound session if one, else null.
   */
  public static EntityManager unbind(final EntityManagerFactory factory) {
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
    Map<Object, EntityManager> emMap =
      context.get();
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
