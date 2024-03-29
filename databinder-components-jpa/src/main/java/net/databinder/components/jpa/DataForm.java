/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2006
 * Nathan Hamblen nathan@technically.us This library is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version. This library is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.databinder.components.jpa;

import java.io.Serializable;

import javax.persistence.EntityManager;

import net.databinder.jpa.Databinder;
import net.databinder.models.jpa.JPAObjectModel;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Provides default handling for a single {@link JPAObjectModel} nested in
 * a {@link CompoundPropertyModel}. This includes saving a new model object to
 * persistent storage and committing the current transaction when a valid form
 * is submitted. For forms holding multiple independent persistent objects (when
 * there is no single parent that cascades saves to the others), subclasses may
 * override {@link #savePersistentObjectIfNew()} to save all the form's
 * {@link JPAObjectModel}s. (Note that automatic {@link #version} tracking
 * is only available for the primary model.)
 * <p>
 * For very specialized forms it may be necessary to extend this class's parent,
 * {@link DataFormBase}.
 * </p>
 * @author Nathan Hamblen
 */
public class DataForm<T> extends DataFormBase<T> {
  private static final long serialVersionUID = 1L;
  /**
   * Retains the persistent object's version field (if it has one) between
   * requests to detect editing conflicts between users.
   * @see #validate()
   */
  private Serializable version;

  /**
   * Instantiates this form and a new, blank instance of the given class as a
   * persistent model object. By default the model object created is serialized
   * and retained between requests until it is persisted.
   * @param id
   * @param modelClass for the persistent object
   * @see JPAObjectModel#setRetainUnsaved(boolean)
   */
  public DataForm(final String id, final Class<T> modelClass) {
    super(id, new CompoundPropertyModel<T>(new JPAObjectModel<T>(modelClass)));
  }

  public DataForm(final String id, final JPAObjectModel<T> model) {
    super(id, new CompoundPropertyModel<T>(model));
    setFactoryKey(model.getFactoryKey());
  }

  /**
   * Instantiates this form with a persistent object of the given class and id.
   * @param id Wicket id
   * @param modelClass for the persistent object
   * @param persistentObjectId id of the persistent object
   */
  public DataForm(final String id, final Class<T> modelClass,
      final Serializable persistentObjectId) {
    super(id, new CompoundPropertyModel<T>(new JPAObjectModel<T>(modelClass,
        persistentObjectId)));
  }

  /**
   * Form that is nested below a component with a compound model containing a
   * JPA model.
   * @param id
   */
  public DataForm(final String id) {
    super(id);
  }

  /**
   * @param key for the JPA EntityManager factory to be used with this component
   * @return this
   */
  @Override
  public DataForm<T> setFactoryKey(final String key) {
    super.setFactoryKey(key);
    getPersistentObjectModel().setFactoryKey(key);
    return this;
  }

  /**
   * @return the single persistent model for this form
   */
  public JPAObjectModel<T> getPersistentObjectModel() {
    return (JPAObjectModel<T>) getCompoundModel().getChainedModel();
  }

  /**
   * Change the persistent model object of this form.
   * @param object to attach to this form
   * @return this form, for chaining
   */
  public DataForm setPersistentObject(final T object) {
    getPersistentObjectModel().setObject(object);
    modelChanged();
    return this;
  }

  /**
   * Updates the internal version number to the actual version number.
   * @see #version
   */
  private void updateVersion() {
    version = getPersistentObjectModel().getVersion();
  }

  /** Late-init version record. */
  @Override
  protected void onBeforeRender() {
    super.onBeforeRender();
    if (version == null) {
      updateVersion();
    }
  }

  /** Calls {@link #updateVersion()} when model changes. */
  @Override
  protected void onModelChanged() {
    updateVersion();
  }

  /**
   * Replaces the form's model object with a new, blank (unbound) instance. Does
   * not affect persistent storage.
   * @see JPAObjectModel#unbind()
   * @return this form, for chaining
   */
  public DataForm clearPersistentObject() {
    getPersistentObjectModel().unbind();
    modelChanged();
    return this;
  }

  /**
   * @return the effective compound model for this form, which may be attached
   *         to a parent component
   */
  protected CompoundPropertyModel getCompoundModel() {
    IModel model = getModel();
    Component cur = this;
    while (cur != null) {
      model = cur.getDefaultModel();
      if (model != null && model instanceof CompoundPropertyModel) {
        return (CompoundPropertyModel) model;
      }
      cur = cur.getParent();
    }
    throw new WicketRuntimeException("DataForm has no parent compound model");
  }

  /** Default implementation calls {@link #commitFormIfValid()}. */
  @Override
  protected void onSubmit() {
    commitFormIfValid();
  }

  /**
   * Commits a valid form's data to persistent storage. If no errors are
   * registered for any form component, this method calls
   * {@link #savePersistentObjectIfNew()} {@link #commitTransactionIfValid()},
   * and {@link #updateVersion()}.
   * @return true if committed
   */
  protected boolean commitFormIfValid() {
    if (!hasError()) {
      savePersistentObjectIfNew();
      commitTransactionIfValid(); // flush and commit EntityManager
      // if version is present it should have changed
      if (version != null) {
        updateVersion();
      }
      return true;
    }
    return false;
  }

  /**
   * Saves persistent model object if it is not already contained in the
   * EntityManager. If the a sub-class is responsible for more than one
   * {@link JPAObjectModel}, it may override to call
   * {@link #saveIfNew(JPAObjectModel)} on each.
   * @return true if object was newly saved
   */
  protected boolean savePersistentObjectIfNew() {
    return saveIfNew(getPersistentObjectModel());
  }

  /**
   * Saves model's entity if it is not already contained in the EntityManager.
   * @return true if object was newly saved
   */
  protected boolean saveIfNew(final JPAObjectModel<T> model) {
    final EntityManager em = Databinder.getEntityManager();
    if (!em.contains(model.getObject())) {
      onBeforeSave(model);
      em.persist(model.getObject());
      // updating binding status; though it will happen on detach
      // some UI components may like to know sooner.
      getPersistentObjectModel().checkBinding();
      return true;
    }
    return false;
  }

  /**
   * Called before saving any new object by {@link #saveIfNew(JPAObjectModel)}.
   * This is a good time to make last minute changes to new objects that
   * couldn't be easily serialized (adding relationships to existing persistent
   * entities, for example).
   * @param generally, the persistent model for this form (but subclasses may
   *          also call saveIfNew)
   */
  protected void onBeforeSave(final JPAObjectModel<T> model) {
  };

  /**
   * Checks that the version number, if present, is the last known version
   * number. If it does not match, validation fails and will continue to fail
   * until the form is reloaded with the updated data and version number. This
   * allows the user to preserve her unsaved changes while preventing
   * overwrites.
   * <p>
   * <b>Note:</b> although timestamp versions are supported, beware of rounding
   * errors. equals() must return true when comparing the retained version
   * object to the one loaded from persistent storage.
   */
  @Override
  protected void onValidate() {
    if (version != null) {
      final Serializable currentVersion =
        getPersistentObjectModel().getVersion();
      if (!version.equals(currentVersion)) {
        error(getString("version.mismatch", null)); // report error
        // do not update version number as old data still appears in form
      }
    }
    super.onValidate();
  }

  /**
   * @return persistent storage version number if available, null otherwise
   */
  protected Serializable getVersion() {
    return version;
  }

  /**
   * Deletes the form's model object from persistent storage. Flushes change so
   * that queries executed in the same request (e.g., in a JPAListModel)
   * will not return this object.
   * @return true if the object was deleted, false if it did not exist
   */
  protected boolean deletePersistentObject() {
    final EntityManager em = Databinder.getEntityManager();
    final Object modelObject = getPersistentObjectModel().getObject();
    if (!em.contains(modelObject)) {
      return false;
    }
    em.remove(modelObject);
    em.flush();
    return true;
  }

  /**
   * Instances of this nested class call #
   * {@link DataForm#clearPersistentObject()} on their instantiating DataForm
   * when clicked.
   */
  public class ClearLink extends Link {
    /** */
    private static final long serialVersionUID = 1L;

    public ClearLink(final String id) {
      super(id);
    }

    /** @return true if visible and the form's perisistent model is bound */
    @Override
    public boolean isEnabled() {
      return !DataForm.this.isVisibleInHierarchy()
      || getPersistentObjectModel().isBound();
    }

    @Override
    public void onClick() {
      clearPersistentObject();
      DataForm.this.setVisible(true);
    }
  }
}