package net.databinder.models.jpa;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ---*/

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;

import org.apache.wicket.model.IModel;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

/**
 * An OrderingCriteriaBuilder implementation that can be wired to a SearchPanel
 * or similar and the given properties searched auto-magically via an iLike.
 * Avoids problems with duplicate Aliases by having all the Criteria building
 * code in one location. Example usage; SearchPanel searchPanel = new
 * SearchPanel("search") { public void onUpdate(AjaxRequestTarget target) {
 * target.addComponent(getDataTable()); } }; // ... IModel searchModel = new
 * Model() { public Object getObject() { return searchPanel.getSearch(); } };
 * CriteriaSearchAndSort builder = new CriteriaSearchAndSort(searchModel, new
 * String[]{"name", "category.name"}, new String[]{ "name" }, true, false);
 * SortableHibernateProvider provider = new
 * SortableHibernateProvider(getBeanClass(), builder); DataTable table = new
 * DataTable("table", columns, provider, 25);
 * @author Mark Southern
 */
public class CriteriaSearchAndSort<T> extends CriteriaBuildAndSort<T> {

  private static final long serialVersionUID = 1L;

  private final String[] searchProperties;

  private final IModel<T> searchTextModel;

  public CriteriaSearchAndSort(final IModel<T> searchTextModel,
      final String[] searchProperties, final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased,
      final Class<T> entityClass) {
    super(defaultSortProperty, sortAscending, sortCased, entityClass);
    this.searchTextModel = searchTextModel;
    this.searchProperties = searchProperties;
  }

  @Override
  public void buildUnordered(final CriteriaBuilder criteria) {
    super.buildUnordered(criteria);

    final String searchText = (String) searchTextModel.getObject();
    if (searchText != null) {
      final String[] items = searchText.split("\\s+");
      final Conjunction conj = Restrictions.conjunction();

      final List<String> properties = new ArrayList<String>();
      for (final String prop : getSearchProperties()) {
        properties.add(processProperty(criteria, prop));
      }

      for (final String item : items) {
        final Disjunction dist = Restrictions.disjunction();
        for (final String prop : properties) {
          dist.add(Restrictions.ilike(prop, item, MatchMode.ANYWHERE));
        }
        conj.add(dist);
      }
      // TODO
      // criteria..add(conj);
    }
  }

  public String[] getSearchProperties() {
    return searchProperties;
  }
}