package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

import org.apache.wicket.model.IModel;

/**
 * Uses specified fonts for bold and italic text. By default bold and italic fonts will be derived
 * from the RenderedLabel base font, but if that font is loaded from a resource
 * these must be as well. Links are attributed as underlined plain-weight text in Color.BLUE.
 * @author Nathan Hamblen
 */
public class FontFormattedRenderedLabel extends RenderedLabel {
	private Font italicFont = getFont().deriveFont(Font.ITALIC);
	private Font boldFont = getFont().deriveFont(Font.BOLD);

	public FontFormattedRenderedLabel(final String id) {
		super(id);
	}

	public FontFormattedRenderedLabel(final String id, final IModel model) {
		super(id, model);
	}

	public FontFormattedRenderedLabel(final String id, final boolean shareResource) {
		super(id, shareResource);
	}

	public FontFormattedRenderedLabel(final String id, final IModel model, final boolean shareResource) {
		super(id, model, shareResource);
	}

	public static void loadSharedResources(final String text, final Font font, final Font boldFont, final Font italicFont, final Color color, final Color backgroundColor, final Integer maxWidth) {
		loadSharedResources(new FontFormattedRenderedImageResource(), text, font, boldFont, italicFont, color, backgroundColor, maxWidth);
	}

	protected static void loadSharedResources(final FontFormattedRenderedImageResource res, final String text, final Font font, final Font boldFont, final Font italicFont, final Color color, final Color backgroundColor, final Integer maxWidth) {
		res.boldFont = boldFont;
		res.italicFont = italicFont;
		RenderedLabel.loadSharedResources(res, text, font, color, backgroundColor, maxWidth);
	}

	@Override
	protected FontFormattedRenderedImageResource newRenderedTextImageResource(final boolean isShared) {
		final FontFormattedRenderedImageResource res = new FontFormattedRenderedImageResource();
		//TODO res.setCacheable(isShared);
		res.setState(this);
		return res;
	}



	protected static class FontFormattedRenderedImageResource extends FormattedRenderedTextImageResource {
		protected Font boldFont, italicFont;

		@Override
		public void setState(final RenderedLabel label) {
			final FontFormattedRenderedLabel ffLabel = (FontFormattedRenderedLabel) label;
			boldFont = ffLabel.getBoldFont();
			italicFont = ffLabel.getItalicFont();
			super.setState(label);
		}

		@Override
		void attributeBold(final AttributedString string, final int start, final int end) {
			string.addAttribute(TextAttribute.FONT, boldFont, start, end);
		}
		@Override
		void attributeItalic(final AttributedString string, final int start, final int end) {
			string.addAttribute(TextAttribute.FONT, italicFont, start, end);
		}
		/** Renders as underlined plain-weight text in Color.BLUE; override for other attributes. */
		@Override
		void attributeLink(final AttributedString string, final int start, final int end) {
			string.addAttribute(TextAttribute.UNDERLINE,TextAttribute.UNDERLINE_ON, start, end);
			string.addAttribute(TextAttribute.FOREGROUND,Color.BLUE, start, end);
		}
	}

	public Font getItalicFont() {
		return italicFont;
	}

	public void setItalicFont(final Font italicFont) {
		this.italicFont = italicFont;
	}

	public Font getBoldFont() {
		return boldFont;
	}

	public void setBoldFont(final Font boldFont) {
		this.boldFont = boldFont;
	}
}