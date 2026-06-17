package com.geoguessrrs.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * World-map pin that snaps to the map edge when the target is off-screen,
 * following the same pattern as ClueScrollWorldMapPoint.
 */
public class GeoguessrMapPoint extends WorldMapPoint
{
	private static final Color FILL    = Color.YELLOW;
	private static final Color OUTLINE = new Color(160, 120, 0);

	private static final BufferedImage PIN  = buildPin();
	private static final BufferedImage DOT  = buildDot();
	// anchor the tip of the pin (bottom-centre) to the WorldPoint
	private static final Point PIN_ANCHOR = new Point(PIN.getWidth() / 2, PIN.getHeight());

	public GeoguessrMapPoint(WorldPoint worldPoint, String name)
	{
		super(worldPoint, PIN);
		setImagePoint(PIN_ANCHOR);
		setSnapToEdge(true);
		setJumpOnClick(true);
		setName(name);
		setTooltip(name);
	}

	@Override
	public void onEdgeSnap()
	{
		setImage(DOT);
		setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		setImage(PIN);
		setImagePoint(PIN_ANCHOR);
	}

	// -------------------------------------------------------------------------

	private static BufferedImage buildPin()
	{
		final int w = 14, h = 20;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// circular head
		g.setColor(FILL);
		g.fillOval(1, 1, w - 2, w - 2);
		g.setColor(OUTLINE);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(1, 1, w - 2, w - 2);

		// tapered body pointing down to tip
		int[] px = {w / 2 - 3, w / 2 + 3, w / 2};
		int[] py = {w - 3,      w - 3,      h - 1};
		g.setColor(FILL);
		g.fillPolygon(px, py, 3);
		g.setColor(OUTLINE);
		g.drawPolygon(px, py, 3);

		// centre dot
		g.setColor(Color.BLACK);
		g.fillOval(w / 2 - 2, w / 2 - 2, 5, 5);

		g.dispose();
		return img;
	}

	private static BufferedImage buildDot()
	{
		final int s = 10;
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(FILL);
		g.fillOval(1, 1, s - 2, s - 2);
		g.setColor(OUTLINE);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(1, 1, s - 2, s - 2);
		g.dispose();
		return img;
	}
}
