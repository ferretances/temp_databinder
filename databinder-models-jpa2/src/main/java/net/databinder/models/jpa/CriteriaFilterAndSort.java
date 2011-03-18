package net.databinder.models.jpa;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu

 * Databinder: a simple bridge from Wicket to Hibernate
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 ---*/

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.lang.PropertyResolver;
import org.apache.wicket.util.lang.PropertyResolverConverter;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

/**
 * An OrderingCriteriaBuilder implementation that can be wired to a
 * FilterToolbar String properties are searched via an iLike. Number properties
 * can specify >, >=, < or <= Example usage (from baseball player example);
 * CriteriaFilterAndSort builder = new CriteriaFilterAndSort(new Player(),
 * "nameLast", true, false); FilterForm form = new FilterForm("form", builder);
 * HibernateProvider provider = new HibernateProvider(Player.class, builder);
 * provider.setWrapWithPropertyModel(false); DataTable table = new
 * DataTable("players", columns, provider, 25) {
 * @Override protected Item newRowItem(String id, int index, IModel model) {
 *           return new OddEvenItem(id, index, model); } };
 *           table.addTopToolbar(new AjaxNavigationToolbar(table));
 *           table.addTopToolbar(new FilterToolbar(table, form, builder));
 *           table.addTopToolbar(new AjaxFallbackHeadersToolbar(table,
 *           builder));
 * @author Mark Southern
 */
public class CriteriaFilterAndSort<T> extends CriteriaBuildAndSort implements
IFilterStateLocator<T> {

  private static final long serialVersionUID = 1L;

  // whitespace, a qualifier, a number surrounded by whitespace
  private final Pattern pattern = Pattern
  .compile("^(\\s+)?([><]=?)(\\s+)?(.*)(\\s+)?");

  private Map<String, String> filterMap = new HashMap<String, String>();

  private final Object bean;

  public CriteriaFilterAndSort(final Object bean,
      final String defaultSortProperty, final boolean sortAscending,
      final boolean sortCased, final Class<T> entityClass) {
    super(defaultSortProperty, sortAscending, sortCased, entityClass);
    this.bean = bean;
  }

  @Override
  public void buildUnordered(
      final javax.persistence.criteria.CriteriaBuilder criteria) {
    super.buildUnordered(criteria);

    final Conjunction conj = Restrictions.conjunction();

    for (final Map.Entry<String, String> entry : filterMap.entrySet()) {
      // System.out.println(String.format("%s\t%s", entry.getKey(),
      // entry.getValue()));
      final String property = entry.getKey();
      String value = entry.getValue();
      if (value == null) {
        continue;
      }

      final String prop = processProperty(criteria, property);
      final Class clazz = PropertyResolver.getPropertyClass(property, bean);

      if (String.class.isAssignableFrom(clazz)) {
        final String[] items = value.split("\\s+");
        for (final String item : items) {
          final Disjunction dist = Restrictions.disjunction();
          dist.add(Restrictions.ilike(prop, item, MatchMode.ANYWHERE));
          conj.add(dist);
        }
      } else if (Number.class.isAssignableFrom(clazz)) {
        try {
          final Matcher matcher = pattern.matcher(value);
          if (matcher.matches()) {
            final String qualifier = matcher.group(2);
            value = matcher.group(4);
            final Number num = convertToNumber(value, clazz);
            if (">".equals(qualifier)) {
              conj.add(Restrictions.gt(prop, num));
            } else if ("<".equals(qualifier)) {
              conj.add(Restrictions.lt(prop, num));
            } else if (">=".equals(qualifier)) {
              conj.add(Restrictions.ge(prop, num));
            } else if ("<=".equals(qualifier)) {
              conj.add(Restrictions.le(prop, num));
            }
          } else {
            conj.add(Restrictions.eq(prop, convertToNumber(value, clazz)));
          }
        } catch (final ConversionException ex) {
          // ignore filter in this case
        }
      } else if (Boolean.class.isAssignableFrom(clazz)) {
        conj.add(Restrictions.eq(prop, Boolean.parseBoolean(value)));
      }
    }
    // TODO
    // criteria.add(conj);
  }

  protected Number convertToNumber(final String value, final Class clazz) {
    return (Number) new PropertyResolverConverter(Application.get()
        .getConverterLocator(), Session.get().getLocale())
    .convert(value, clazz);
  }

  @Override
  public T getFilterState() {
    return (T) filterMap;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setFilterState(final Object filterMap) {
    this.filterMap = (Map) filterMap;
  }

}