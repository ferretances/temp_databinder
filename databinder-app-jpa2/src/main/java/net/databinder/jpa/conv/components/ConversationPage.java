package net.databinder.jpa.conv.components;

import java.util.HashMap;

import javax.persistence.EntityManager;

import org.apache.wicket.markup.html.WebPage;

public class ConversationPage extends WebPage implements IConversationPage {

  private final HashMap<Object, EntityManager> conversationEntityManager =
    new HashMap<Object, EntityManager>();

  public ConversationPage() {
  }

  public ConversationPage(final EntityManager em) {
    setConversationEntityManager(null);
  }

  public ConversationPage(final Object key, final EntityManager em) {
    setConversationEntityManager(key, em);
  }

  public EntityManager getConversationEntityManger(final Object key) {
    return conversationEntityManager.get(key);
  }

  public void setConversationEntityManager(final Object key,
      final EntityManager em) {
    conversationEntityManager.put(key, em);
  }

  public EntityManager getConversationEntityManager() {
    return getConversationEntityManger(null);
  }

  public void setConversationEntityManager(final EntityManager em) {
    setConversationEntityManager(null, em);
  }

}
