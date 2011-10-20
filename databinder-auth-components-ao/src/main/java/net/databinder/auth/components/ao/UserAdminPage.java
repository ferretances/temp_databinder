package net.databinder.auth.components.ao;

import java.io.Serializable;
import java.util.Map;

import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.UserAdminPageBase;
import net.databinder.auth.data.ao.DataUserEntity;
import net.databinder.auth.data.ao.UserHelper;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityListModel;
import net.databinder.models.ao.EntityModel;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;

/**
 * User administration page. Lists all users, allows editing usernames, passwords, and roles.
 * Must have Role.ADMIN to view. Replaceable String resources: <pre>
 * data.auth.user_admin
 * data.auth.user_add
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.roles
 * data.auth.save
 * data.auth.delete</pre>
 * @see net.databinder.auth.AuthSession
 */

public class UserAdminPage<T extends DataUserEntity<K>, K extends Serializable>
		extends UserAdminPageBase<T> {
	private DataForm<T, K> form;

	@Override
	protected Form<T> adminForm(final String id, final Class<T> userClass) {
		return form = new DataForm<T, K>(id, new EntityModel<T, K>(userClass) {
			@Override
			protected void putDefaultProperties(
					final Map<String, Object> propertyStore) {
				propertyStore.put("roles", new Roles(Roles.USER));
			}
		}) {
			@Override
			protected void onSubmit() {
				if (!getEntityModel().isBound()) {
					final Map<String, Object> map = (Map<String, Object>) getModelObject();
					map.put("roleString", ((Roles)map.remove("roles")).toString());
				}
				super.onSubmit();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setPassword(final String password) {
		if (form.getEntityModel().isBound()) {
			super.setPassword(password);
		} else {
			((Map)getUserForm().getModelObject()).put("passwordHash", UserHelper.getHash(password));
		}
	}

	@Override
	protected Button deleteButton(final String id) {
		return form.new DeleteButton(id);
	}

	@Override
	protected DataUserStatusPanelBase statusPanel(final String id) {
		return new DataUserStatusPanel(id);
	}

	@Override
	protected EntityListModel<T> userList(final Class<T> userClass) {
		return new EntityListModel<T>(userClass);
	}

}
