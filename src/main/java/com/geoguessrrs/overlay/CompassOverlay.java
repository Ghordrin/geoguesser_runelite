package com.geoguessrrs.overlay;

import com.geoguessrrs.GeoguessrPlugin;
import com.geoguessrrs.GeoguessrState;
import com.geoguessrrs.GameMode;
import com.geoguessrrs.round.Round;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class CompassOverlay extends Overlay
{
	private static final int ARROW_SIZE = 40;
	private static final int PANEL_WIDTH = 110;
	private static final int PANEL_HEIGHT = 90;
	private static final Color BG_COLOR = new Color(0, 0, 0, 160);
	private static final Color ARROW_COLOR = new Color(0xFF5C5C);
	private static final Color TEXT_COLOR = Color.WHITE;

	private final Client client;
	private final GeoguessrPlugin plugin;

	private GeoguessrState state = GeoguessrState.IDLE;
	private Round activeRound;

	@Inject
	CompassOverlay(Client client, GeoguessrPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
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
		if (state != GeoguessrState.ACTIVE || plugin.getActiveGameMode() != GameMode.HUNT || activeRound == null)
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		WorldPoint pos = player.getWorldLocation();
		WorldPoint target = new WorldPoint(
			activeRound.getLocation().getX(),
			activeRound.getLocation().getY(),
			activeRound.getLocation().getPlane()
		);

		int dist = pos.distanceTo2D(target);
		int dx = target.getX() - pos.getX();
		int dy = target.getY() - pos.getY();
		// getCameraYaw() = 0 when camera faces north, increases counter-clockwise (0-2047)
		// (turning right toward east gives yaw ~1536, not ~512 — hence + not -)
		double cameraYawRad = client.getCameraYaw() * 2.0 * Math.PI / 2048.0;
		double angle = Math.atan2(-dy, dx) + cameraYawRad;

		// Background
		graphics.setColor(BG_COLOR);
		graphics.fillRoundRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 8, 8);

		// Arrow centred in panel
		int cx = PANEL_WIDTH / 2;
		int cy = ARROW_SIZE / 2 + 10;
		drawArrow(graphics, cx, cy, angle);

		// Distance text
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(new Font("Arial", Font.BOLD, 12));
		graphics.setColor(TEXT_COLOR);
		String distText = dist + " tiles";
		int textW = graphics.getFontMetrics().stringWidth(distText);
		graphics.drawString(distText, cx - textW / 2, cy + ARROW_SIZE / 2 + 16);

		// Time
		String timeText = activeRound.getElapsedSeconds() + "s";
		int timeW = graphics.getFontMetrics().stringWidth(timeText);
		graphics.setColor(new Color(180, 180, 180));
		graphics.drawString(timeText, cx - timeW / 2, cy + ARROW_SIZE / 2 + 30);

		return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
	}

	private void drawArrow(Graphics2D graphics, int cx, int cy, double angle)
	{
		int half = ARROW_SIZE / 2;
		Path2D arrow = new Path2D.Double();
		// Arrow pointing right at angle=0; rotated by angle
		arrow.moveTo(half, 0);
		arrow.lineTo(-half + 8, -10);
		arrow.lineTo(-half + 16, 0);
		arrow.lineTo(-half + 8, 10);
		arrow.closePath();

		AffineTransform tx = AffineTransform.getTranslateInstance(cx, cy);
		tx.rotate(angle); // angle already accounts for canvas Y-flip via atan2(-dy, dx)
		arrow.transform(tx);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ARROW_COLOR);
		graphics.fill(arrow);
		graphics.setColor(ARROW_COLOR.darker());
		graphics.setStroke(new BasicStroke(1.5f));
		graphics.draw(arrow);
	}
}
