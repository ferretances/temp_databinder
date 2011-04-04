package net.databinder.components.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import net.databinder.jpa.Databinder;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Base class for forms that commit in onSubmit(). This is extended by DataForm,
 * and may be extended directly by client forms when DataForm is not
 * appropriate. Transactions are committed only when no errors are displayed.
 * @author Nathan Hamblen
 */
public class DataFormBase<T> extends Form<T> {

  private static final long serialVersionUID = 1L;

  private String factoryKey;

  public DataFormBase(final String id) {
    super(id);
  }

  public DataFormBase(final String id, final IModel<T> model) {
    super(id, model);
  }

  public Object getFactoryKey() {
    return factoryKey;
  }

  public DataFormBase<T> setFactoryKey(final String key) {
    this.factoryKey = key;
    return this;
  }

  protected EntityManager getEntityManager() {
    return net.databinder.jpa.Databinder.getEntityManager(factoryKey);
  }

  /** Default implementation calls {@link #commitTransactionIfValid()}. */
  @Override
  protected void onSubmit() {
    commitTransactionIfValid();
  }

  /**
   * Commit transaction if no errors are registered for any form component.
   * @return true if transaction was committed
   */
  protected boolean commitTransactionIfValid() {
    try {
      if (!hasError()) {
        final EntityManager em = Databinder.getEntityManager(factoryKey);
        em.flush(); // needed for conv. EntityManagers, harmless otherwise
        onBeforeCommit();
        em.getTransaction().commit();
        em.getTransaction().begin();
        return true;
      }
    } catch (final PersistenceException e) {
      error(getString("version.mismatch", null)); // report error
    }
    return false;
  }

  /**
   * Called before committing a transaction by
   * {@link #commitTransactionIfValid()}.
   */
  protected void onBeforeCommit() {
  };

}
