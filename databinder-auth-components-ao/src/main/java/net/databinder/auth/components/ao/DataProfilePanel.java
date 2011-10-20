package net.databinder.auth.components.ao;

import java.io.Serializable;
import java.util.Map;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.auth.data.ao.DataUserEntity;
import net.databinder.auth.data.ao.UserHelper;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityModel;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Registration with username, password, and password confirmation.
 * Replaceable String resources: <pre>
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.remember
 * data.auth.register
 * data.auth.update
 * data.auth.username.taken * </pre> * Must be overriden in a containing page
 * or a subclass of this panel.
 */
public class DataProfilePanel<T extends DataUserEntity<K>, K extends Serializable>
		extends DataProfilePanelBase<T> {

	public DataProfilePanel(final String id, final ReturnPage returnPage) {
		super(id, returnPage);
	}

	private DataForm<T, K> form;

	@Override
	protected Form<T> profileForm(final String id, IModel<T> userModel) {
		if (userModel == null) {
			userModel = (IModel<T>) new EntityModel<T, K>(((AuthApplication<T>)getApplication()).getUserClass());
		}
		return form = new DataForm<T, K>(id, (EntityModel<T, K>) userModel) {
			@SuppressWarnings("unchecked")
			@Override
			protected void onSubmit() {
				if (!getEntityModel().isBound()) {
					final Map<String, Object> map = (Map) getModelObject();
					map.put("roleString", Roles.USER);
				}
				super.onSubmit();
			}
			@Override
			protected void afterSubmit() {
				DataProfilePanel.this.afterSubmit();
			}
		};
	}

	/**
	 * Uses super implementation if bound, but for new users this method must call
	 * UserHelper.getHash(password) to set the hash in "passwordHash". If application
	 * uses different hasing implementation.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void setPassword(final String password) {
		if (form.getEntityModel().isBound()) {
			super.setPassword(password);
		} else {
			((Map)form.getModelObject()).put("passwordHash", UserHelper.getHash(password));
		}
	}

}
