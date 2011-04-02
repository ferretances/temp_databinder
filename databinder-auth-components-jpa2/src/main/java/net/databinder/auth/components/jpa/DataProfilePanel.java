package net.databinder.auth.components.jpa;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.auth.data.DataUser;
import net.databinder.components.jpa.DataForm;
import net.databinder.models.jpa.JPAObjectModel;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Registration with username, password, and password confirmation. Replaceable
 * String resources:
 * 
 * <pre>
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.remember
 * data.auth.register
 * data.auth.update
 * data.auth.username.taken *
 * </pre>
 * 
 * * Must be overriden in a containing page or a subclass of this panel.
 */
public class DataProfilePanel<T extends DataUser> extends
DataProfilePanelBase<T> {

  private static final long serialVersionUID = 1L;

  public DataProfilePanel(final String id, final ReturnPage returnPage) {
    super(id, returnPage);
  }

  @Override
  protected Form<T> profileForm(final String id, IModel<T> userModel) {
    if (userModel == null) {
      userModel =
        new JPAObjectModel<T>(
            ((AuthApplication) getApplication()).getUserClass());
    }

    return new DataForm<T>(id, (JPAObjectModel<T>) userModel) {
      @Override
      protected void onSubmit() {
        super.onSubmit();
        DataProfilePanel.this.afterSubmit();
      }
    };
  }

}
