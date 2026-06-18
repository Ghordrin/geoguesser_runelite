package com.geoguessrrs.overlay;

import com.geoguessrrs.GeoguessrState;
import com.geoguessrrs.GameMode;
import com.geoguessrrs.GeoguessrPlugin;
import com.geoguessrrs.round.RoundResult;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.input.MouseAdapter;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Intercepts world-map mouse clicks in Classic Mode to record the player's guess.
 * Also draws a small "Click to guess!" banner while the world map is open.
 *
 * NOTE: The world-map widget lookup and coordinate conversion need in-game verification.
 */
public class WorldMapGuessOverlay extends Overlay
{
	private final Client client;
	private final GeoguessrPlugin plugin;

	// Written from the client thread, read from the render thread and EDT — must be volatile.
	private volatile GeoguessrState state = GeoguessrState.IDLE;
	private volatile Consumer<WorldPoint> onGuess;
	private volatile RoundResult lastResult;

	@Inject
	WorldMapGuessOverlay(Client client, GeoguessrPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	public void setState(GeoguessrState state, Consumer<WorldPoint> onGuess)
	{
		this.state = state;
		this.onGuess = onGuess;
	}

	public void setResult(RoundResult result)
	{
		this.lastResult = result;
	}

	/** Mouse listener registered with MouseManager when plugin starts. */
	public final net.runelite.client.input.MouseListener mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mouseClicked(MouseEvent e)
		{
			if (state != GeoguessrState.ACTIVE || plugin.getActiveGameMode() != GameMode.CLASSIC)
			{
				return e;
			}

			WorldPoint guess = canvasToWorldPoint(e.getX(), e.getY());
			if (guess != null && onGuess != null)
			{
				onGuess.accept(guess);
				return null; // consume
			}
			return e;
		}
	};

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Rectangle mapBounds = getMapWidgetBounds();
		if (mapBounds == null)
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Active: "click to guess" banner
		if (state == GeoguessrState.ACTIVE && plugin.getActiveGameMode() == GameMode.CLASSIC)
		{
			int bx = mapBounds.x + mapBounds.width / 2 - 90;
			int by = mapBounds.y + 10;
			graphics.setColor(new Color(0, 0, 0, 180));
			graphics.fillRoundRect(bx, by, 180, 24, 6, 6);
			graphics.setColor(Color.YELLOW);
			graphics.setFont(new Font("Arial", Font.BOLD, 12));
			graphics.drawString("Click to place your guess!", bx + 8, by + 16);
		}

		// Result: line from guess to target
		if (lastResult != null)
		{
			java.awt.Point guessPixel  = worldPointToCanvas(lastResult.getGuess(),  mapBounds);
			java.awt.Point targetPixel = worldPointToCanvas(lastResult.getTarget(), mapBounds);

			if (guessPixel != null && targetPixel != null)
			{
				graphics.setClip(mapBounds);

				// Line
				graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				graphics.setColor(new Color(255, 220, 0, 200));
				graphics.drawLine(guessPixel.x, guessPixel.y, targetPixel.x, targetPixel.y);

				// Distance label at midpoint
				int mx = (guessPixel.x + targetPixel.x) / 2;
				int my = (guessPixel.y + targetPixel.y) / 2;
				String distLabel = lastResult.getDistance() + " tiles";
				graphics.setFont(new Font("Arial", Font.BOLD, 11));
				FontMetrics fm = graphics.getFontMetrics();
				int lw = fm.stringWidth(distLabel);
				graphics.setColor(new Color(0, 0, 0, 160));
				graphics.fillRoundRect(mx - lw / 2 - 3, my - fm.getAscent(), lw + 6, fm.getHeight() + 2, 4, 4);
				graphics.setColor(Color.WHITE);
				graphics.drawString(distLabel, mx - lw / 2, my);

				// Guess marker — green circle
				drawMapMarker(graphics, guessPixel, new Color(0x22BB44), "YOU");

				// Target marker — red circle
				drawMapMarker(graphics, targetPixel, new Color(0xDD2222), "TARGET");

				graphics.setClip(null);
			}
		}

		return null;
	}

	private static void drawMapMarker(Graphics2D g, java.awt.Point p, Color color, String label)
	{
		int r = 7;
		g.setColor(color.darker());
		g.fillOval(p.x - r - 1, p.y - r - 1, (r + 1) * 2, (r + 1) * 2);
		g.setColor(color);
		g.fillOval(p.x - r, p.y - r, r * 2, r * 2);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 9));
		FontMetrics fm = g.getFontMetrics();
		g.drawString(label, p.x - fm.stringWidth(label) / 2, p.y + fm.getAscent() / 2 - 1);
	}

	private Rectangle getMapWidgetBounds()
	{
		net.runelite.api.widgets.Widget widget = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
		if (widget == null || widget.isHidden())
		{
			return null;
		}
		return widget.getBounds();
	}

	private java.awt.Point worldPointToCanvas(WorldPoint wp, Rectangle mapBounds)
	{
		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null) return null;

		Point mapCenter = worldMap.getWorldMapPosition();
		float zoom = worldMap.getWorldMapZoom();

		int canvasCenterX = mapBounds.x + mapBounds.width / 2;
		int canvasCenterY = mapBounds.y + mapBounds.height / 2;

		int x = canvasCenterX + Math.round((wp.getX() - mapCenter.getX()) * zoom);
		int y = canvasCenterY - Math.round((wp.getY() - mapCenter.getY()) * zoom);
		return new java.awt.Point(x, y);
	}

	private WorldPoint canvasToWorldPoint(int canvasX, int canvasY)
	{
		Rectangle mapBounds = getMapWidgetBounds();
		if (mapBounds == null || !mapBounds.contains(canvasX, canvasY))
		{
			return null;
		}

		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null)
		{
			return null;
		}

		Point mapCenter = worldMap.getWorldMapPosition();
		float zoom = worldMap.getWorldMapZoom();

		int canvasCenterX = mapBounds.x + mapBounds.width / 2;
		int canvasCenterY = mapBounds.y + mapBounds.height / 2;

		int tileOffsetX = Math.round((canvasX - canvasCenterX) / zoom);
		int tileOffsetY = Math.round(-(canvasY - canvasCenterY) / zoom);

		return new WorldPoint(
			mapCenter.getX() + tileOffsetX,
			mapCenter.getY() + tileOffsetY,
			0
		);
	}
}
