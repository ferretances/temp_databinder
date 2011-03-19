package net.databinder.jpa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.HibernateException;

public class ManagedEntityManagerContext implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final ThreadLocal context = new ThreadLocal();

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
      throw new HibernateException(
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
    return (EntityManager) entityManagerMap(true).put(
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
    final Map emMap = entityManagerMap();
    if (emMap != null) {
      existing = (EntityManager) emMap.remove(factory);
      doCleanup();
    }
    return existing;
  }

  private static EntityManager existingEntityManager(
      final EntityManagerFactory emf) {
    final Map emMap = entityManagerMap();
    if (emMap == null) {
      return null;
    } else {
      return (EntityManager) emMap.get(emf);
    }
  }

  protected static Map entityManagerMap() {
    return entityManagerMap(false);
  }

  private static synchronized Map entityManagerMap(final boolean createMap) {
    Map emMap = (Map) context.get();
    if (emMap == null && createMap) {
      emMap = new HashMap();
      context.set(emMap);
    }
    return emMap;
  }

  private static synchronized void doCleanup() {
    final Map emMap = entityManagerMap(false);
    if (emMap != null) {
      if (emMap.isEmpty()) {
        context.set(null);
      }
    }
  }
}
