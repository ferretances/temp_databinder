package net.databinder.models.ao;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.databinder.ao.Databinder;
import net.databinder.models.BindingModel;
import net.databinder.models.LoadableWritableModel;
import net.java.ao.Common;
import net.java.ao.RawEntity;
import net.java.ao.schema.FieldNameConverter;

import org.apache.wicket.util.string.Strings;

public class EntityModel<T extends RawEntity<K>, K extends Serializable>
		extends LoadableWritableModel<Object> implements BindingModel<Object> {
	private K id;
	private Class<T> entityType;
	private Map<String, Object> propertyStore;
	private Object managerKey;

	public EntityModel(final Class<T> entityType, final K id) {
		this(entityType);
		this.id = id;
	}

	public EntityModel(final Class<T> entityType) {
		this.entityType = entityType;
	}

	public EntityModel(final T entity) {
		setObject(entity);
	}

	public boolean isBound() {
		return id != null;
	}

	@Override
	protected Object load() {
		if (isBound()) {
			return Databinder.getEntityManager(managerKey).get(entityType, id);
		}
		return getPropertyStore();
	}

	@SuppressWarnings("unchecked")
	public void setObject(final Object entity) {
		unbind();
		entityType = (Class<T>) ((T)entity).getEntityType();
		id = Common.getPrimaryKeyValue((T)entity);
		setTempModelObject(entity);
	}

	protected void putDefaultProperties(final Map<String, Object> propertyStore) { }

	public void unbind() {
		id = null;
		propertyStore = null;
		detach();
	}

	/**
	 * @return map of  properties to values for Wicket property models
	 */
	public Map<String, Object> getPropertyStore() {
		if (propertyStore == null) {
			propertyStore = new HashMap<String, Object>();
			putDefaultProperties(propertyStore);
		}
		return propertyStore;
	}

	/**
	 * @return map of database fields to their values for creating new entities
	 */
	public Map<String, Object> getFieldMap() {
		final Map<String, Object> properties = getPropertyStore(), fields = new HashMap<String, Object>();
		final FieldNameConverter conv = Databinder.getEntityManager(managerKey).getFieldNameConverter();
		for (final Entry<String, Object> e : properties.entrySet()) {
			String field = e.getKey();
			final String prop = Strings.capitalize(field);
			for (final Method m : entityType.getMethods()) {
				// match getter or setter
				if (m.getName().substring(3).equals(prop)) {
					field = conv.getName(m);
					break;
				}
			}
			if (e.getValue() != null) {
				fields.put(field, e.getValue());
			}
		}
		return fields;
	}

	public Class<T> getEntityType() {
		return entityType;
	}

	public Object getManagerKey() {
		return managerKey;
	}

	public void setManagerKey(final Object managerKey) {
		this.managerKey = managerKey;
	}

}
