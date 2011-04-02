package net.databinder.auth.components.jpa;

import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.auth.components.DataUserStatusPanelBase;

import org.apache.wicket.markup.html.WebPage;

/**
 * Displays sign in and out links, as well as current user if any. Replaceable
 * String resources:
 * 
 * <pre>
 * data.auth.status.account
 * data.auth.status.admin
 * data.auth.status.sign_out
 * data.auth.status.sign_in
 * </pre>
 */
public class DataUserStatusPanel extends DataUserStatusPanelBase {

  public DataUserStatusPanel(final String id) {
    super(id);
  }

  @Override
  protected WebPage profilePage(final ReturnPage returnPage) {
    return new DataProfilePage(returnPage);
  }

  @Override
  protected Class<? extends WebPage> adminPageClass() {
    return UserAdminPage.class;
  }
}
