package net.databinder.auth.components.jpa;

import net.databinder.auth.components.DataProfilePageBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

import org.apache.wicket.Component;

/**
 * Display profile editing page for logged in user. Replaceable String
 * resources:
 * 
 * <pre>
 * data.auth.update
 * data.auth.title.update
 */
public class DataProfilePage extends DataProfilePageBase {
  public DataProfilePage(final ReturnPage returnPage) {
    super(returnPage);
  }

  @Override
  protected Component profileSocket(final String id, final ReturnPage returnPage) {
    return new DataProfilePanel(id, returnPage);
  }
}
