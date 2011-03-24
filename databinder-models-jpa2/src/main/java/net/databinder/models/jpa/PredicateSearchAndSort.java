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

import static net.databinder.util.JPAUtil.propertyStringExpressionToPath;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Predicate;

import org.apache.wicket.model.IModel;

public class PredicateSearchAndSort<T> extends PredicateBuildAndSort<T> {

  private static final long serialVersionUID = 1L;

  private final String[] searchProperties;

  private final IModel<T> searchTextModel;

  public PredicateSearchAndSort(final IModel<T> searchTextModel,
      final String[] searchProperties, final String defaultSortProperty,
      final boolean sortAscending, final boolean sortCased,
      final Class<T> entityClass) {
    super(defaultSortProperty, sortAscending, sortCased, entityClass);
    this.searchTextModel = searchTextModel;
    this.searchProperties = searchProperties;
  }

  @Override
  public void buildUnordered(final List<Predicate> criteria) {
    super.buildUnordered(criteria);

    final String searchText = (String) searchTextModel.getObject();
    if (searchText != null) {
      final String[] items = searchText.split("\\s+");

      final List<String> properties = new ArrayList<String>();
      for (final String prop : getSearchProperties()) {
        properties.add(processProperty(criteria, prop));
      }

      for (final String item : items) {
        for (final String prop : properties) {
          final Predicate p =
            cb.like(cb.lower(propertyStringExpressionToPath(root, prop)),
                item);
          criteria.add(p);
        }
      }
      cq.where(cb.and(criteria.toArray(new Predicate[0])));
    }
  }

  public String[] getSearchProperties() {
    return searchProperties;
  }
}