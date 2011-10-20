/*
 * Databinder: a simple bridge from Wicket to JPA Copyright (C) 2008
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
package net.databinder.valid.jpa;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;

/**
 * Checks a base model and property name against JPA Validator.
 * @author Nathan Hamblen
 * @author Rodolfo Hansen
 * @param <T> Type parameter for the validator.
 */
public class DatabinderValidator<T> extends AbstractValidator<T> {
  private static final long serialVersionUID = 1L;

  /** JPA Validator to use. */
  private Validator validator;

  /** base model, may be null until first call to onValidate. */
  private IModel<T> base;
  /** property of base to validate, may be null until first call to onValidate. */
  private String property;
  /** component added to */
  private FormComponent<T> component;

  /**
   * Validator for a property of an entity.
   * @param base entity to validate
   * @param property property of base to validate
   * @param validator validator to validate with
   */
  public DatabinderValidator(final IModel<T> base, final String property,
      final Validator validator) {
    this.base = base;
    this.property = property;
    this.validator = validator;
  }

  /**
   * Validator for a property of an entity.
   * @param base entity to validate
   * @param property property of base to validate
   */
  public DatabinderValidator(final IModel<T> base, final String property) {
    this.base = base;
    this.property = property;
  }

  /**
   * Construct instance that attempts to determine the base object and property
   * to validate form the component it is added to. This is only possible for
   * components that depend on a parent CompoundPropertyModel or their own
   * PropertyModels. The attempt is not made until the first validation check in
   * {@link #onValidate(IValidatable)} (to allow the full component hierarchy to
   * be constructed). Do not use an instance for more than one component.
   */
  public DatabinderValidator() {
  }

  /**
   * Gets the <tt>validator</tt>.
   * @return the validator
   */
  public Validator getValidator() {
    return validator;
  }

  /**
   * Sets the <tt>validator</tt>.
   * @param validator the validator to set
   * @return the DatabinderValidator object (builder ideology).
   */
  public DatabinderValidator<?> setValidator(final Validator validator) {
    this.validator = validator;
    return this;
  }

  /**
   * Checks the component against JPA Validator. If the base model and
   * property were not supplied in the constructor, they will be determined from
   * the component this validator was added to.
   */
  @Override
  protected void onValidate(final IValidatable<T> comp) {
    if (base == null || property == null) {
      final ModelProp<T> mp = getModelProp(component);
      base = mp.model;
      property = mp.prop;
    }
    final Object o = base.getObject();
    if (validator == null) {
      validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    for (final ConstraintViolation<Object> cv : validator.validate(o)) {
      final ValidationError vError = new ValidationError();
      comp.error(vError.setMessage(cv.getInvalidValue() + " " + cv.getMessage()));
    }
  }

  /**
   * Retains component for possible use in onValidate.
   * @param component component assigned to this validator.
   */
  @SuppressWarnings("unchecked")
  public void onAdded(final Component component) {
    this.component = (FormComponent<T>) component;
  }

  /** @return always true */
  @Override
  public boolean validateOnNullValue() {
    return true;
  }

  private static class ModelProp<T> {
    IModel<T> model;
    String prop;
  }

  /** @return base object and property derived from this component */
  @SuppressWarnings("unchecked")
  private static <T> ModelProp<T> getModelProp(
      final FormComponent<T> formComponent) {
    final IModel<T> model = formComponent.getModel();
    final ModelProp<T> mp = new ModelProp<T>();
    if (model instanceof PropertyModel) {
      final PropertyModel<T> propModel = (PropertyModel<T>) model;
      mp.model = (IModel<T>) propModel.getChainedModel();
      mp.prop = propModel.getPropertyExpression();
    } else if (model instanceof IWrapModel) {
      mp.model = ((IWrapModel) model).getWrappedModel();
      mp.prop = formComponent.getId();
    } else {
      throw new UnrecognizedModelException(formComponent, model);
    }
    return mp;
  }

  /**
   * Add immediately to a form component. Note that the component's model object
   * must be available for inspection at this point or an exception will be
   * thrown. (For a CompoundPropertyModel, this means the hierarchy must be
   * established.) This is only possible for components that depend on a parent
   * CompoundPropertyModel or their own PropertyModels.
   * @param <T> Type Safe implementation for any given FormComponent.
   * @param formComponent component to add validator to
   * @throws UnrecognizedModelException if no usable model is present
   */
  public static <T> void addTo(final FormComponent<T> formComponent) {
    final ModelProp<T> mp = getModelProp(formComponent);
    formComponent.add(new DatabinderValidator<T>(mp.model, mp.prop));
  }

  /**
   * Add immediately to a form component. Note that the component's model object
   * must be available for inspection at this point or an exception will be
   * thrown. (For a CompoundPropertyModel, this means the hierarchy must be
   * established.) This is only possible for components that depend on a parent
   * CompoundPropertyModel or their own PropertyModels.
   * @param <T> Type Safe implementation for any given FormComponent.
   * @param formComponent component to add validator to
   * @param validator ClassValidator for the newly asigned instance
   * @throws UnrecognizedModelException if no usable model is present
   */
  public static <T> void addTo(final FormComponent<T> formComponent,
      final Validator validator) {
    final ModelProp<T> mp = getModelProp(formComponent);
    formComponent.add(new DatabinderValidator<T>(mp.model, mp.prop, validator));
  }

  public static class UnrecognizedModelException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnrecognizedModelException(final Component formComponent,
        final IModel<?> model) {
      super("DatabinderValidator doesn't recognize the model " + model
          + " of component " + formComponent.toString());
    }
  }
}
