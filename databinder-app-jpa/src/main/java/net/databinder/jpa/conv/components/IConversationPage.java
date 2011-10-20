package net.databinder.jpa.conv.components;

import javax.persistence.EntityManager;

public interface IConversationPage {

  public EntityManager getConversationEntityManger(Object key);

  public void setConversationEntityManager(Object key, EntityManager em);
}
