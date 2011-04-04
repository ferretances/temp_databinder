/*
 * Databinder: a simple bridge from Wicket to JPA
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

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
 */
package net.databinder.components;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * Panel for a comma-separated list of tag strings displayed in a text field or text area. The
 * backing model for this component must be a Set of String tags. Tags are converted to
 * lower case before insertion to the set.
 */
public class TagField extends Panel {

  /** Generates text field */
  public TagField(final String id) {
    this(id, null);
  }
  /** Generates text field */
  public TagField(final String id, final IModel model) {
    this(id, model, false);
  }
  /** @param textarea true to generate a text area */
  public TagField(final String id, final boolean textarea) {
    this(id, null, textarea);
  }
  /** @param textarea true to generate a text area */
  public TagField(final String id, final IModel model, final boolean textarea) {
    super(id, model);
    final IModel<String> tagModel = new IModel<String>() {
      public void detach() {}
      @SuppressWarnings("unchecked")
      public String getObject() {
        final Collection<String> tags = (Collection<String>) TagField.this.getDefaultModelObject();
        if (tags == null) {
          return null;
        }
        return Strings.join(", ",  tags.toArray(new String[tags.size()]));
      }
      public void setObject(final String object) {
        if (object == null) {
          TagField.this.setDefaultModelObject(new HashSet<String>());
        } else {
          final String value = object.toLowerCase();
          final String[] tagstrs = value.split(" *, *,* *"); // also consumes empty ' ,  ,' tags
          TagField.this.setDefaultModelObject(new HashSet<String>(Arrays.asList(tagstrs)));
        }
      }
    };
    add(new TextField<String>("field", tagModel).setVisible(!textarea));
    add(new TextArea<String>("area", tagModel).setVisible(textarea));
  }
}
