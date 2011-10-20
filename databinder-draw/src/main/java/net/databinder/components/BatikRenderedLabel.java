package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * Renders text without font hinting, which can be better for some fonts
 * at larger sizes. The standard Java pipeline does not allow hinting to
 * be disabled; Batik does.
 * @author Nathan Hamblen
 */
public class BatikRenderedLabel extends RenderedLabel {
	public BatikRenderedLabel(final String id) {
		super(id);
	}
	public BatikRenderedLabel(final String id, final boolean shareResource) {
		super(id, shareResource);
	}
	public BatikRenderedLabel(final String id, final IModel model) {
		super(id, model);
	}
	public BatikRenderedLabel(final String id, final IModel model, final boolean shareResource) {
		super(id, model, shareResource);
	}


	public static void loadSharedResources(final String text, final Font font, final Color color, final Color backgroundColor, final Integer maxWidth) {
		loadSharedResources(new BatikRenderedTextImageResource(), text, font, color, backgroundColor, maxWidth);
	}

	@Override
	protected RenderedTextImageResource newRenderedTextImageResource(final boolean isShared) {
		final RenderedTextImageResource res = new BatikRenderedTextImageResource();
//		res.setCacheable(isShared);
		res.setState(this);
		return res;
	}


	protected static class BatikRenderedTextImageResource extends RenderedTextImageResource {

		@Override
		protected List<AttributedCharacterIterator> getAttributedLines() {
			if (Strings.isEmpty(text)) {
				return null;
			}
			final AttributedString attributedText = new AttributedString(text);

			final List<GVTFont> fonts = new ArrayList<GVTFont>(1);
			fonts.add(new AWTGVTFont(font));
			attributedText.addAttribute(StrokingTextPainter.GVT_FONTS, fonts);

			final TextPaintInfo tpi = new TextPaintInfo();
			tpi.visible = true;
			tpi.fillPaint = color;
			attributedText.addAttribute(StrokingTextPainter.PAINT_INFO, tpi);

			return splitAtNewlines(attributedText, text);
		}

		@Override
		protected boolean render(final Graphics2D graphics) {
			final int width = getWidth(), height = getHeight();

			// draw background if not null, otherwise leave transparent
			if (backgroundColor != null) {
				graphics.setColor(backgroundColor);
				graphics.fillRect(0, 0, width, height);
			}

			// render as a 1x1 pixel if text is empty
			if (Strings.isEmpty(text)) {
				if (width == 1 && height == 1) {
					return true;
				}
				setWidth(1);
				setHeight(1);
				return false;
			}

			// Get size of text
			graphics.setFont(font);
			final FontMetrics fontMetrics = graphics.getFontMetrics();

			final List<AttributedCharacterIterator> attributedLines = getAttributedLines();

			// each one of these is needed for a unhinted, anti-aliased display
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
					RenderingHints.VALUE_STROKE_PURE);

			 // TODO: maxwidth wrapping layout, format string processing

			final float lineHeight = fontMetrics.getHeight(),
				spare = fontMetrics.getMaxAscent() - fontMetrics.getAscent()
					+ fontMetrics.getMaxDescent() - fontMetrics.getDescent(),
				neededHeight = attributedLines.size() * lineHeight + spare;
			float neededWidth = 0f, y = fontMetrics.getMaxAscent();

			for (final AttributedCharacterIterator line : attributedLines) {
				final TextNode node = new TextNode();
				node.setLocation(new Point(0, (int) y));
				node.setAttributedCharacterIterator(line);
				node.getTextPainter().paint(node, graphics);

				final float w = (float) node.getTextPainter().getBounds2D(node).getWidth() + 4f;
				if (w > neededWidth) {
					neededWidth = w;
				}

				y += lineHeight;
			}
			if (neededWidth > width || neededHeight > height) {
				setWidth((int)Math.ceil(neededWidth));
				setHeight((int)Math.ceil(neededHeight));
				return false;
			}

			return true;
		}
	}

}
