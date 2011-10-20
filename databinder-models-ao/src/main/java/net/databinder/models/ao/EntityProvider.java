package net.databinder.models.ao;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;

import net.databinder.ao.Databinder;
import net.databinder.models.PropertyDataProvider;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.WicketObjects;

@SuppressWarnings("unchecked")
public class EntityProvider extends PropertyDataProvider {

	private final Class entityType;
	private final Query query;
	private Object managerKey;

	public EntityProvider(final Class entityType) {
		this (entityType, Query.select());
	}

	public EntityProvider(final Class entityType, final Query query) {
		this.entityType = entityType;
		this.query = query;
	}

	public Iterator iterator(final int first, final int count) {
		try {
			final Query q = ((Query) WicketObjects.cloneObject(query)).offset(first).limit(count);

			return Arrays.asList(Databinder.getEntityManager(managerKey).find(entityType, q)).iterator();
		} catch (final SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}

	public int size() {
		try {
			return Databinder.getEntityManager(managerKey).count(entityType, query);
		} catch (final SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}

	@Override
	protected IModel dataModel(final Object object) {
		return new EntityModel((RawEntity)object);
	}

	@Override
	public void detach() { }

	public Object getManagerKey() {
		return managerKey;
	}

	public void setManagerKey(final Object managerKey) {
		this.managerKey = managerKey;
	}
}
