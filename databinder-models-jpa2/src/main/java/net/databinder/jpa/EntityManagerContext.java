package net.databinder.jpa;

import javax.persistence.EntityManager;

public interface EntityManagerContext {

  EntityManager bind(final EntityManager em);

  boolean hasBind();

  EntityManager currentEntityManager();

  EntityManager unbind();

}
