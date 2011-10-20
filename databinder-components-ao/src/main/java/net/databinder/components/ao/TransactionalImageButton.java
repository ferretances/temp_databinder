package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.ImageButton;
import org.apache.wicket.request.resource.ResourceReference;

/** Button that provides a submit transaction similar to TransactionalForm. */
public abstract class TransactionalImageButton extends ImageButton {
	public TransactionalImageButton(final String id, final ResourceReference resourceReference) {
		super(id, resourceReference);
	}

	/** Called when the form is submitted, do not override if you want transactional behavior. */
	@Override
	public void onSubmit() {
		try {
			new Transaction<Object>(Databinder.getEntityManager()) {
				@Override
				protected Object run() throws SQLException {
					inSubmitTransaction(Databinder.getEntityManager());
					return null;
				}
			}.execute();
		} catch (final SQLException e) {
			throw new WicketRuntimeException(e);
		}
		afterSubmit();
	}

	/**
	 * Called inside onSubmit's database transaction.
	 * @param entityManager associated with button, provided for convenience
	 */
	protected abstract void inSubmitTransaction(EntityManager entityManager) throws SQLException;
	/** Called after onSubmit's database transaction. */
	protected void afterSubmit() { };
}
