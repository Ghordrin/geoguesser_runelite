package com.geoguessrrs.capture;

import com.geoguessrrs.GeoguessrConfig;
import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.SpritePixels;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Slf4j
public class CaptureOverlay extends Overlay
{
	private static final int PREVIEW_SIZE        = 100;
	private static final int SCENE_TILES         = 104;
	private static final int PIXELS_PER_TILE     = 4;
	private static final int SCENE_SIZE          = SCENE_TILES * PIXELS_PER_TILE;
	private static final int CAPTURE_RADIUS      = 44;
	private static final int ICON_REFRESH_TICKS  = 10;
	private static final int VIEWPORT_REFRESH_TICKS = 30; // ~18 s

	private final Client client;
	private final BatchCaptureManager batchManager;
	private final GeoguessrConfig config;

	private BufferedImage cachedPreview;
	private BufferedImage cachedIconLayer;
	private int iconLayerTick     = Integer.MIN_VALUE;
	private int viewportPreviewTick = Integer.MIN_VALUE;
	private volatile int batchDistance = -1;

	@Inject
	CaptureOverlay(Client client, BatchCaptureManager batchManager, GeoguessrConfig config)
	{
		this.client       = client;
		this.batchManager = batchManager;
		this.config       = config;
		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.LOW);
	}

	/** Called from onGameTick (client thread) to update the distance-to-batch-target indicator. */
	public void setBatchDistance(int distance)
	{
		this.batchDistance = distance;
	}

	/** Called from onGameTick to refresh the preview (not every frame). */
	public void refreshPreview()
	{
		if (config.captureViewport())
		{
			refreshViewportPreview();
		}
		else
		{
			refreshMinimapPreview();
		}
	}

	// ── Preview implementations ───────────────────────────────────────────────

	private void refreshMinimapPreview()
	{
		SpritePixels sprite = client.drawInstanceMap(client.getTopLevelWorldView().getPlane());
		if (sprite == null) { cachedPreview = null; return; }

		int w = sprite.getWidth();
		int h = sprite.getHeight();
		if (w < SCENE_SIZE || h < SCENE_SIZE) { cachedPreview = null; return; }

		Player player = client.getLocalPlayer();
		if (player == null) { cachedPreview = null; return; }

		int padX = (w - SCENE_SIZE) / 2;
		int padY = (h - SCENE_SIZE) / 2;

		LocalPoint lp = player.getLocalLocation();
		int playerPixelX = lp.getSceneX() * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
		int playerPixelY = (SCENE_TILES - 1 - lp.getSceneY()) * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;

		BufferedImage raw = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		raw.setRGB(0, 0, w, h, sprite.getPixels(), 0, w);

		int tick = client.getTickCount();
		if (cachedIconLayer == null || (tick - iconLayerTick) >= ICON_REFRESH_TICKS)
		{
			cachedIconLayer = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D iconG = cachedIconLayer.createGraphics();
			MinimapIconPainter.paint(iconG, client, client.getTopLevelWorldView().getPlane());
			iconG.dispose();
			iconLayerTick = tick;
		}

		BufferedImage scene = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sceneG = scene.createGraphics();
		sceneG.drawImage(raw.getSubimage(padX, padY, SCENE_SIZE, SCENE_SIZE), 0, 0, null);
		sceneG.drawImage(cachedIconLayer, 0, 0, null);
		sceneG.dispose();

		int diameter = CAPTURE_RADIUS * 2;
		int cropX = Math.max(0, Math.min(playerPixelX - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		int cropY = Math.max(0, Math.min(playerPixelY - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		cachedPreview = CircleMask.apply(scene.getSubimage(cropX, cropY, diameter, diameter));
	}

	/**
	 * Captures the game viewport via Robot every VIEWPORT_REFRESH_TICKS ticks and
	 * crops it to the same initial-hint radius the player would see in round 1.
	 * The overlay UI itself will be visible in the preview — accepted artefact.
	 */
	private void refreshViewportPreview()
	{
		int tick = client.getTickCount();
		if (cachedPreview != null && (tick - viewportPreviewTick) < VIEWPORT_REFRESH_TICKS) return;

		try
		{
			Canvas canvas = client.getCanvas();
			Point loc = canvas.getLocationOnScreen();
			int w = client.getCanvasWidth();
			int h = client.getCanvasHeight();
			if (w <= 0 || h <= 0) { cachedPreview = null; return; }

			BufferedImage full = new Robot().createScreenCapture(new Rectangle(loc.x, loc.y, w, h));

			// Match the same proportional crop startRound() applies: 30% of max radius
			int maxR    = Math.min(w, h) / 2;
			int initialR = Math.max(26, maxR * 3 / 10);
			int diam    = initialR * 2;
			int cropX   = Math.max(0, w / 2 - initialR);
			int cropY   = Math.max(0, h / 2 - initialR);
			diam = Math.min(diam, Math.min(w - cropX, h - cropY));

			cachedPreview = CircleMask.apply(full.getSubimage(cropX, cropY, diam, diam));
			viewportPreviewTick = tick;
		}
		catch (AWTException e)
		{
			log.debug("Viewport preview capture failed", e);
			cachedPreview = null;
		}
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int y = 0;

		if (cachedPreview != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.drawImage(cachedPreview, 0, 0, PREVIEW_SIZE, PREVIEW_SIZE, null);
			graphics.setColor(config.captureViewport() ? Color.GREEN : Color.YELLOW);
			graphics.drawOval(0, 0, PREVIEW_SIZE - 1, PREVIEW_SIZE - 1);
			y = PREVIEW_SIZE + 2;
		}

		BatchCaptureManager.BatchTarget bt = batchManager.getCurrent();
		if (bt != null)
		{
			int idx   = batchManager.getCurrentIndex() + 1;
			int total = batchManager.getTotal();

			graphics.setColor(Color.CYAN);
			graphics.drawString(String.format("BATCH %d/%d", idx, total), 2, y + 10);

			String name = bt.getName();
			if (name.length() > 16) name = name.substring(0, 15) + "…";
			graphics.drawString(name, 2, y + 22);

			if (batchDistance >= 0)
			{
				graphics.setColor(batchDistance <= 5 ? Color.GREEN : Color.YELLOW);
				graphics.drawString(batchDistance + " tiles", 2, y + 34);
			}
			return new Dimension(PREVIEW_SIZE, y + 38);
		}

		graphics.setColor(Color.YELLOW);
		graphics.drawString("CAPTURE MODE", 2, y + 12);
		return new Dimension(PREVIEW_SIZE, y + 14);
	}
}
