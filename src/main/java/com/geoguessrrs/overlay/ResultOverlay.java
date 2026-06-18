package com.geoguessrrs.overlay;

import com.geoguessrrs.round.RoundResult;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class ResultOverlay extends Overlay
{
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 104;
	private static final Color BG_COLOR = new Color(0, 0, 0, 200);
	private static final long SHOW_DURATION_MS = 8000;

	// Written from the client thread, read from the render thread — must be volatile.
	private volatile RoundResult result;
	private volatile long shownAt;

	public ResultOverlay()
	{
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	public void show(RoundResult result)
	{
		this.result  = result;
		this.shownAt = System.currentTimeMillis();
	}

	public void hide()
	{
		this.result = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (result == null)
		{
			return null;
		}
		if (System.currentTimeMillis() - shownAt > SHOW_DURATION_MS)
		{
			result = null;
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setColor(BG_COLOR);
		graphics.fillRoundRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 10, 10);

		graphics.setFont(new Font("Arial", Font.BOLD, 14));
		graphics.setColor(Color.YELLOW);
		graphics.drawString("Round Complete!", 10, 20);

		graphics.setFont(new Font("Arial", Font.PLAIN, 12));
		graphics.setColor(Color.WHITE);
		graphics.drawString("Score:    " + result.getScore(), 10, 38);
		graphics.drawString("Distance: " + result.getDistance() + " tiles", 10, 54);
		graphics.drawString("Time:     " + result.getElapsedSeconds() + "s", 10, 70);
		graphics.drawString("Hints:    " + result.getHintsUsed(), 10, 86);

		String teleport = result.getNearestTeleport();
		if (teleport != null)
		{
			graphics.setColor(new Color(0xAADDFF));
			graphics.drawString("✈ " + teleport, 10, 102);
		}

		return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
	}
}
