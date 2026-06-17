package com.geoguessrrs.overlay;

import com.geoguessrrs.GeoguessrState;
import com.geoguessrrs.round.Round;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class TargetTileOverlay extends Overlay
{
	private static final Color FILL   = new Color(255, 255, 0, 60);
	private static final Color BORDER = new Color(255, 215, 0, 220);
	private static final BasicStroke STROKE = new BasicStroke(2f);

	private final Client client;

	private GeoguessrState state = GeoguessrState.IDLE;
	private Round activeRound;

	@Inject
	TargetTileOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.MED);
	}

	public void setState(GeoguessrState state, Round round)
	{
		this.state = state;
		this.activeRound = round;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (state != GeoguessrState.ACTIVE || activeRound == null)
		{
			return null;
		}

		WorldPoint target = new WorldPoint(
			activeRound.getLocation().getX(),
			activeRound.getLocation().getY(),
			activeRound.getLocation().getPlane()
		);

		if (target.getPlane() != client.getTopLevelWorldView().getPlane())
		{
			return null;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, target);
		if (lp == null)
		{
			return null; // target outside loaded scene
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null)
		{
			return null;
		}

		graphics.setColor(FILL);
		graphics.fillPolygon(poly);
		graphics.setColor(BORDER);
		graphics.setStroke(STROKE);
		graphics.drawPolygon(poly);

		return null;
	}
}
