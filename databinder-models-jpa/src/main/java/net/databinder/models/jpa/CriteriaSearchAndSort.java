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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.wicket.model.IModel;

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
  public void buildUnordered(final CriteriaBuilder cb) {
    super.buildUnordered(cb);

    final String searchText = (String) searchTextModel.getObject();
    if (searchText != null) {
      final String[] items = searchText.split("\\s+");

      final List<String> properties = new ArrayList<String>();
      for (final String prop : getSearchProperties()) {
        properties.add(processProperty(cb, prop));
      }
      final List<Predicate> crit = new ArrayList<Predicate>();
      final Root<T> root = cb.createQuery().from(entityClass);

      for (final String item : items) {
        for (final String prop : properties) {
          final Predicate p =
            cb.like(cb.lower(propertyStringExpressionToPath(root, prop)),
                item);
          crit.add(p);
        }
      }
    }
  }

  public String[] getSearchProperties() {
    return searchProperties;
  }
}