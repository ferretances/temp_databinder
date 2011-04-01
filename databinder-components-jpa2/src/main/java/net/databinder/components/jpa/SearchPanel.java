package net.databinder.components.jpa;

import static net.databinder.util.JPAUtil.propertyStringExpressionToPath;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import net.databinder.components.AjaxCell;
import net.databinder.components.AjaxOnKeyPausedUpdater;
import net.databinder.models.jpa.BasicPredicateBuilder;
import net.databinder.models.jpa.PredicateBuilder;
import net.databinder.models.jpa.PropertyQueryBinder;
import net.databinder.util.CriteriaDefinition;
import net.databinder.util.JPAUtil;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Panel for a "live" search field with a clear button. Instances of this class
 * must implement the onUpdate method to register external components for
 * updating. It is possible to override the search button text with the key
 * "searchbutton.text" The SearchPanel model maps to the text of the search.
 * @author Nathan Hamblen
 */
public abstract class SearchPanel<T extends Serializable> extends Panel {

  private static final long serialVersionUID = 1L;

  private TextField<String> search;

  /**
   * @param id Wicket id
   */
  public SearchPanel(final String id) {
    super(id, new Model<String>());
    add(new SearchForm("searchForm"));
  }

  /** Use the given model (must not be read-only ) for the search string */
  public SearchPanel(final String id, final IModel<String> searchModel) {
    super(id, searchModel);
    add(new SearchForm("searchForm"));
  }

  @Override
  /** Sets model to search component. */
  public MarkupContainer setDefaultModel(final IModel<?> model) {
    return search.setDefaultModel(model);
  }

  /**
   * Override to add components to be updated (or JavaScript to be executed)
   * when the search string changes. Remember that added components must have a
   * markup id; use component.setMarkupId(true) to assign one programmatically.
   * @param target Ajax target to register components for update
   */
  public abstract void onUpdate(AjaxRequestTarget target);

  /**
   * Binds the search model to a "search" parameter in a query. The value in the
   * search field will be bracketed by percent signs (%) for a find-anywhere
   * match. In the query itself, "search" must be the name of the one and only
   * parameter If your needs differ, bind the model passed in to the SearchPanel
   * constructor to your own IQueryBinder instance; this is a convenience
   * method.
   * @return binder for a "search" parameter
   */
  public net.databinder.models.jpa.QueryBinder getQueryBinder(
      final String[] params) {
    return new PropertyQueryBinder(this, params);
  }

  /**
   * Adds a criterion that will match the current search string anywhere within
   * any of the given properties. If the search is empty, no criterion is added.
   * @param searchProperty one or more properties to be searched
   * @return builder to be used with list model or data provider
   */
  public PredicateBuilder<T> getCriteriaBuilder(final Class<T> entityClass,
      final String... searchProperty) {
    return new BasicPredicateBuilder<T>(entityClass) {
      private static final long serialVersionUID = 1L;

      @Override
      public void build(final List<Predicate> predicates) {
        final String search = (String) getDefaultModelObject();
        if (search != null) {
          final CriteriaDefinition<T> cd = getCriteriaDefinition();
          final CriteriaBuilder cb = cd.getCriteriaBuilder();
          final Root<T> root = cd.getRoot();
          for (final String prop : searchProperty) {
            final Predicate p =
              cb.like(cb.lower(propertyStringExpressionToPath(root, prop)),
                  getSearch());
            cd.addPredicate(p);
          }
        }
      }

      @Override
      protected Class<T> getEntityClass() {
        return entityClass;
      }
    };
  }

  /** @return search string bracketed by the % wildcard */
  public String getSearch() {
    final Object defaultModelObject = getDefaultModelObject();
    return defaultModelObject == null ? null : JPAUtil
        .likePattern(defaultModelObject.toString());
  }

  /**
   * Resets the search model, The default behavior is setting it to null, but it
   * is possible to override with your own custom defaults.
   */
  public void resetSearchModelObject() {
    setDefaultModelObject(null);
  }

  /** Form with AJAX components and their AjaxCells. */
  public class SearchForm extends Form<Object> {
    /** */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public SearchForm(final String id) {
      super(id);

      final AjaxCell searchWrap = new AjaxCell("searchWrap");
      add(searchWrap);
      search =
        new TextField<String>("searchInput",
            (IModel<String>) SearchPanel.this.getDefaultModel());
      search.setOutputMarkupId(true);
      searchWrap.add(search);

      final AjaxCell clearWrap = new AjaxCell("clearWrap");
      add(clearWrap);
      final AjaxLink<?> clearLink = new AjaxLink<Object>("clearLink") {
        /** */
        private static final long serialVersionUID = 1L;

        /** Clear field and register updates. */
        @Override
        public void onClick(final AjaxRequestTarget target) {
          resetSearchModelObject();
          target.addComponent(searchWrap);
          target.addComponent(clearWrap);
          onUpdate(target);
        }

        /** Hide when search is blank. */
        @Override
        public boolean isVisible() {
          return SearchPanel.this.getDefaultModelObject() != null;
        }
      };
      clearLink.setOutputMarkupId(true);
      clearLink.add(new Image("clear", new ResourceReference(this.getClass(),
      "clear.png")));
      clearWrap.add(clearLink);

      // triggered when user pauses or tabs out
      search.add(new AjaxOnKeyPausedUpdater() {
        /** */
        private static final long serialVersionUID = 1L;

        @Override
        protected void onUpdate(final AjaxRequestTarget target) {
          target.addComponent(clearWrap);
          SearchPanel.this.onUpdate(target);
        }
      });
    }
  }
}
