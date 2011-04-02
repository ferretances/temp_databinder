package net.databinder.auth.components.jpa;

import java.util.List;

import javax.persistence.EntityManager;

import net.databinder.auth.AuthSession;
import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.UserAdminPageBase;
import net.databinder.auth.data.DataUser;
import net.databinder.components.jpa.DataForm;
import net.databinder.models.jpa.JPAListModel;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * User administration page. Lists all users, allows editing usernames,
 * passwords, and roles. Must have Role.ADMIN to view. Replaceable String
 * resources:
 * 
 * <pre>
 * data.auth.user_admin
 * data.auth.user_add
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.roles
 * data.auth.save
 * data.auth.delete
 * </pre>
 * @see AuthSession
 */
public class UserAdminPage<T extends DataUser> extends UserAdminPageBase<T> {
  private DataForm<T> form;

  @Override
  protected Form<T> adminForm(final String id, final Class<T> userClass) {
    return form = new DataForm<T>(id, userClass);
  }

  @Override
  protected Button deleteButton(final String id) {
    return new Button("delete") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onSubmit() {
        final EntityManager em =
          net.databinder.jpa.Databinder.getEntityManager();
        em.remove(getUserForm().getModelObject());
        em.getTransaction().commit();
        form.clearPersistentObject();
      }

      @SuppressWarnings("unchecked")
      @Override
      public boolean isEnabled() {
        return !((AuthSession<T>) getSession()).getUser().equals(
            getUserForm().getModelObject())
            && getBindingModel().isBound();
      }
    }.setDefaultFormProcessing(false);
  }

  @Override
  protected DataUserStatusPanelBase statusPanel(final String id) {
    return new DataUserStatusPanel(id);
  }

  @Override
  protected IModel<List<T>> userList(final Class<T> userClass) {
    return new JPAListModel<T>(userClass);
  }

}
