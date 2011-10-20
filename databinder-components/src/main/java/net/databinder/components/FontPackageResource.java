package net.databinder.components;

import java.util.Locale;

import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.util.resource.IResourceStream;

public class FontPackageResource extends PackageResource {

	private static final long serialVersionUID = 1L;

	protected FontPackageResource(final Class<?> scope, final String name,
			final Locale locale, final String style, final String variation) {
		super(scope, name, locale, style, variation);
	}

	@Override
	public IResourceStream getResourceStream() {
		return super.getResourceStream();
	}

}
