/*
 * Databinder: a simple bridge from Wicket to Hibernate
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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * Component for a stylesheet link. The stylesheet is expected to be named
 * &lt;ClassName&gt;.css for the class specified in the constructor and be
 * located in the same package as that class.
 *
 * @author Nathan Hamblen
 */

public class StyleLink extends WebMarkupContainer {

	/** Builds a StyleLinkbased on the given class. */
	public StyleLink(final String id, final Class pageClass) {
		super(id);
		add(new Behavior() {
			@Override
			public void renderHead(final Component component,
					final IHeaderResponse response) {
				response.renderCSSReference(new CssResourceReference(pageClass,
						"cssResource"));
			}
		});
	}

	protected StyleLink(final String id, final Class pageClass,
			final String filename) {
		super(id);
		add(new Behavior() {
			@Override
			public void renderHead(final Component component,
					final IHeaderResponse response) {
				response.renderCSSReference(new CssResourceReference(pageClass,
						"cssResource", getLocale(), filename, null));
			}
		});
	}

	/** Sets appropriate href, type, and rel values for the stylesheet. */
	@Override
	protected void onComponentTag(final ComponentTag tag) {
		// ensure valid css tag
		checkComponentTag(tag, "link");
		tag.put("type", "text/css");
		tag.put("rel", "stylesheet");
		super.onComponentTag(tag);
	}
}